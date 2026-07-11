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

// Cloudflare model-catalog id (author/model form) — selects both the provider
// and the model when the AI binding routes through the gateway. Must match a
// model listed at https://developers.cloudflare.com/ai/models/.
const MODEL_ID = "anthropic/claude-haiku-4-5";
// The AI Gateway on this account. Unified Billing must be enabled (credits
// loaded) for the account; the gateway's Spend Limit is the hard cost backstop.
const GATEWAY_ID = "nimaz";

// Gateway errors that mean "no more money", not "the model broke": the
// gateway spend limit tripped or the account's AI credits ran out. Mapped to
// BUDGET_EXCEEDED so the app shows its friendly "resting for now" state.
const OUT_OF_BUDGET = /spend limit|spending limit|insufficient credit|out of credit|no credits/i;

/**
 * Call Claude through the AI binding → `nimaz` AI Gateway → Anthropic, with
 * Unified Billing: Cloudflare injects the provider credentials and bills the
 * account's AI credits. No API key or gateway token lives in the Worker.
 *
 * The Anthropic-native request (system + cache_control, tools, tool_choice,
 * max_tokens, temperature) is forwarded unchanged as the model input; the
 * `model` field inside the body is dropped because the catalog id above
 * selects provider + model.
 */
export async function callClaude(
  request: AnthropicMessagesRequest,
  env: Env,
  metadata?: Record<string, string | number | boolean>,
): Promise<AnthropicResponse> {
  const { model: _ignored, ...input } = request;
  let res: unknown;
  try {
    res = await env.AI.run(MODEL_ID, input, {
      gateway: {
        id: GATEWAY_ID,
        // Shows up per-request in the AI Gateway dashboard (spend breakdown).
        // Never contains question text.
        ...(metadata ? { metadata } : {}),
      },
    });
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    if (OUT_OF_BUDGET.test(msg)) {
      throw new ApiError(
        "BUDGET_EXCEEDED",
        "AI answers are resting for now — the spending limit has been reached. Please try again later.",
      );
    }
    throw new UpstreamError(`AI Gateway call failed: ${msg.slice(0, 500)}`, 502);
  }

  // The binding forwards Anthropic's native response shape (content blocks +
  // usage). Guard defensively in case the gateway normalised it away — the
  // dispatcher needs `content[]` to find the forced tool_use block.
  if (!res || !Array.isArray((res as { content?: unknown }).content)) {
    throw new UpstreamError("Unexpected AI Gateway response shape", 502);
  }
  return res as AnthropicResponse;
}
