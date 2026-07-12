import { Hono, type Context } from "hono";
import type { ContentfulStatusCode } from "hono/utils/http-status";
import { z } from "zod";
import { getCapability, listCapabilityIds } from "./registry";
import { callClaude, UpstreamError } from "./anthropic/client";
import { checkIntegrity } from "./middleware/integrity";
import { ApiError } from "./middleware/errors";

// Envelope shared by every capability invocation.
const InvokeEnvelopeSchema = z.object({
  capability: z.string().min(1),
  integrityToken: z.string(),
  deviceId: z.string().min(8).max(128),
  input: z.unknown(),
});

const app = new Hono<{ Bindings: Env }>();

// Health check — no auth. Used by CI and uptime monitors.
app.get("/v1/health", (c) =>
  c.json({ ok: true, capabilities: listCapabilityIds() }),
);

app.post("/v1/invoke", async (c) => {
  const now = new Date();

  // 0. Parse the envelope.
  let body: unknown;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(
      c,
      new ApiError("INVALID_INPUT", "Request body must be valid JSON."),
    );
  }
  const envelope = InvokeEnvelopeSchema.safeParse(body);
  if (!envelope.success) {
    return errorResponse(
      c,
      new ApiError("INVALID_INPUT", "Malformed request envelope."),
    );
  }
  // deviceId stays in the envelope for compatibility/forward use, but the
  // Worker no longer keeps per-device counters — throttling is the gateway's.
  const { capability: capabilityId, integrityToken, input } = envelope.data;

  const capability = getCapability(capabilityId);
  if (!capability) {
    return errorResponse(
      c,
      new ApiError("INVALID_INPUT", `Unknown capability: ${capabilityId}`),
    );
  }

  try {
    // 1. Play Integrity — the Worker's only guard. Blocks only an explicit
    //    failed verdict; "unavailable" (missing token, unconfigured, Google
    //    outage) fails open. Request throttling and the monthly USD cost cap
    //    both live in the AI Gateway (Rate Limiting rule + Spend Limit);
    //    callClaude maps them to RATE_LIMITED / BUDGET_EXCEEDED.
    const integrity = await checkIntegrity(c.env, integrityToken, now.getTime());
    if (integrity === "failed") {
      throw new ApiError(
        "ATTESTATION_FAILED",
        "This device or app could not be verified.",
      );
    }

    // 2a. Validate capability input.
    const parsedInput = capability.inputSchema.safeParse(input);
    if (!parsedInput.success) {
      throw new ApiError(
        "INVALID_INPUT",
        parsedInput.error.issues[0]?.message ?? "Invalid input.",
      );
    }

    // 2b. Build request + call Claude via the AI binding (Unified Billing).
    //     The capability id rides along as gateway metadata so the AI Gateway
    //     dashboard breaks spend down per feature (never the question text).
    const request = capability.buildRequest(parsedInput.data, c.env);
    const raw = await callClaude(request, c.env, { capability: capabilityId });

    // 2c. Structured usage log (observability): lets us confirm prompt-cache
    //     reads (cache_read_input_tokens > 0) and watch token spend without
    //     ever logging content.
    console.log(
      JSON.stringify({ event: "ai_usage", capability: capabilityId, ...raw.usage }),
    );

    // 2d. Parse + validate output against the strict contract.
    const output = capability.parseResponse(raw, parsedInput.data);
    const validated = capability.outputSchema.safeParse(output);
    if (!validated.success) {
      throw new ApiError(
        "UPSTREAM_ERROR",
        "The model returned an unexpected response shape.",
      );
    }

    // Token usage echoed as a header — nothing sensitive, and it makes the
    // prompt-cache smoke test (cache_read_input_tokens on the 2nd call) a
    // plain curl instead of a dashboard dig.
    c.header("x-nimaz-usage", JSON.stringify(raw.usage));
    return c.json(validated.data, 200);
  } catch (err) {
    if (err instanceof ApiError) return errorResponse(c, err);
    if (err instanceof UpstreamError) {
      // Log + echo the upstream detail: the app only switches on the error
      // `code` (never shows server messages for UPSTREAM_ERROR), and having
      // the gateway's reason in the envelope makes ops/CI failures diagnosable
      // without a dashboard dig. No user content is ever in this message.
      console.error(
        JSON.stringify({
          event: "upstream_error",
          status: err.status,
          message: err.message,
        }),
      );
      return errorResponse(
        c,
        new ApiError(
          "UPSTREAM_ERROR",
          `The AI service is unavailable. [${err.message.slice(0, 300)}]`,
        ),
      );
    }
    // parseResponse throwing (e.g. model didn't call the tool) lands here.
    return errorResponse(
      c,
      new ApiError("UPSTREAM_ERROR", "Failed to generate an answer."),
    );
  }
});

function errorResponse(c: Context<{ Bindings: Env }>, err: ApiError) {
  return c.json(err.toBody(), err.status as ContentfulStatusCode);
}

export default app;
