import type {
  AnthropicMessagesRequest,
  AnthropicResponse,
} from "../capabilities/types";
import { ApiError } from "../middleware/errors";

export class UpstreamError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = "UpstreamError";
  }
}

// Cloudflare model-catalog id (author/model form) — routes the gateway to
// Anthropic with Unified Billing (Cloudflare-managed credentials + credits).
const MODEL_ID = "anthropic/claude-haiku-4-5";
const GATEWAY_ID = "nimaz";
const ACCOUNT_ID = "0e2f38a4dd1f2052809b0d876dcc790e";
// AI Gateway's Anthropic-native REST endpoint: strictly the Anthropic
// Messages schema, so forced tool use and cache_control pass through
// verbatim. (The AI binding path rejected the native input with
// "7003: User Input Error", so the REST endpoint is the supported route.)
const ENDPOINT = `https://api.cloudflare.com/client/v4/accounts/${ACCOUNT_ID}/ai/v1/messages`;
const ANTHROPIC_VERSION = "2023-06-01";

// Gateway errors that mean "no more money", not "the model broke": the
// gateway spend limit tripped or the account's AI credits ran out. Mapped to
// BUDGET_EXCEEDED so the app shows its friendly "resting for now" state.
const OUT_OF_BUDGET =
  /spend limit|spending limit|insufficient credit|out of credit|no credits/i;

/**
 * Call Claude through the `nimaz` AI Gateway with Unified Billing. Auth is a
 * scoped Cloudflare token (AI Gateway Run) — never an Anthropic key; the
 * Anthropic credentials are Cloudflare-managed and spend draws from the
 * account's AI credits. The token is sent both as the API bearer and as
 * cf-aig-authorization so it satisfies the gateway's authenticated-gateway
 * check too.
 */
export async function callClaude(
  request: AnthropicMessagesRequest,
  env: Env,
  metadata?: Record<string, string | number | boolean>,
): Promise<AnthropicResponse> {
  const res = await fetch(ENDPOINT, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      authorization: `Bearer ${env.CLOUDFLARE_AI_TOKEN}`,
      "cf-aig-authorization": `Bearer ${env.CLOUDFLARE_AI_TOKEN}`,
      "cf-aig-gateway-id": GATEWAY_ID,
      "anthropic-version": ANTHROPIC_VERSION,
      // Per-feature spend breakdown in the gateway dashboard. Never contains
      // question text.
      ...(metadata ? { "cf-aig-metadata": JSON.stringify(metadata) } : {}),
    },
    // The catalog id selects provider + model at the gateway; everything else
    // (system + cache_control, tools, tool_choice, max_tokens, temperature)
    // is the Anthropic-native request, forwarded unchanged.
    body: JSON.stringify({ ...request, model: MODEL_ID }),
  });

  if (!res.ok) {
    const body = await res.text().catch(() => "");
    if (OUT_OF_BUDGET.test(body)) {
      throw new ApiError(
        "BUDGET_EXCEEDED",
        "AI answers are resting for now — the spending limit has been reached. Please try again later.",
      );
    }
    throw new UpstreamError(
      `AI Gateway ${res.status}: ${body.slice(0, 500)}`,
      res.status,
    );
  }

  const json = (await res.json()) as AnthropicResponse;
  // The endpoint returns Anthropic's native response shape (content blocks +
  // usage). Guard defensively — the dispatcher needs content[] to find the
  // forced tool_use block.
  if (!json || !Array.isArray((json as { content?: unknown }).content)) {
    throw new UpstreamError("Unexpected AI Gateway response shape", 502);
  }
  return json;
}
