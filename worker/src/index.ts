import { Hono, type Context } from "hono";
import type { ContentfulStatusCode } from "hono/utils/http-status";
import { z } from "zod";
import { getCapability, listCapabilityIds } from "./registry";
import { callClaude, UpstreamError } from "./anthropic/client";
import { verifyIntegrity } from "./middleware/integrity";
import { enforceRateLimit } from "./middleware/rateLimit";
import { enforceBudget, recordSpend } from "./middleware/budgetGuard";
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
  const { capability: capabilityId, integrityToken, deviceId, input } =
    envelope.data;

  const capability = getCapability(capabilityId);
  if (!capability) {
    return errorResponse(
      c,
      new ApiError("INVALID_INPUT", `Unknown capability: ${capabilityId}`),
    );
  }

  try {
    // 1. Integrity (Play Integrity attestation, bypassed when SKIP_ATTESTATION).
    await verifyIntegrity(c.env, integrityToken, now.getTime());

    // 2. Rate limit (per-device + global daily caps).
    await enforceRateLimit(c.env, deviceId, now);

    // 3. Budget guard (pre-call gate).
    await enforceBudget(c.env, now);

    // 4a. Validate capability input.
    const parsedInput = capability.inputSchema.safeParse(input);
    if (!parsedInput.success) {
      throw new ApiError(
        "INVALID_INPUT",
        parsedInput.error.issues[0]?.message ?? "Invalid input.",
      );
    }

    // 4b. Build request + call Claude.
    const request = capability.buildRequest(parsedInput.data, c.env);
    const raw = await callClaude(request, c.env);

    // 4c. Account for spend (post-call).
    await recordSpend(c.env, raw.usage, now);

    // 4d. Parse + validate output against the strict contract.
    const output = capability.parseResponse(raw, parsedInput.data);
    const validated = capability.outputSchema.safeParse(output);
    if (!validated.success) {
      throw new ApiError(
        "UPSTREAM_ERROR",
        "The model returned an unexpected response shape.",
      );
    }

    return c.json(validated.data, 200);
  } catch (err) {
    if (err instanceof ApiError) return errorResponse(c, err);
    if (err instanceof UpstreamError) {
      return errorResponse(
        c,
        new ApiError("UPSTREAM_ERROR", "The AI service is unavailable."),
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
