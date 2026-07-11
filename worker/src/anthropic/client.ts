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

const ACCOUNT_ID = "0e2f38a4dd1f2052809b0d876dcc790e";
const GATEWAY_ID = "nimaz";
// The gateway's Anthropic provider-native endpoint: strictly the Anthropic
// Messages schema (forced tool use + cache_control pass through verbatim) and
// plain Anthropic model names. With no provider key attached, Unified Billing
// injects the Cloudflare-managed Anthropic credentials and bills the
// account's AI credits. Auth is the gateway's own token (cf-aig-authorization,
// enforced by the gateway's Authenticated Gateway setting).
// (Two other transports were live-tested and rejected: the AI binding fails
// the native schema with 7003, and api.cloudflare.com/...:/ai/v1/messages
// rejects this token class with 401/10000.)
const ENDPOINT = `https://gateway.ai.cloudflare.com/v1/${ACCOUNT_ID}/${GATEWAY_ID}/anthropic/v1/messages`;
const ANTHROPIC_VERSION = "2023-06-01";

// Gateway errors that mean "no more money", not "the model broke": the
// gateway spend limit tripped or the account's AI credits ran out. Mapped to
// BUDGET_EXCEEDED so the app shows its friendly "resting for now" state.
const OUT_OF_BUDGET =
  /spend limit|spending limit|insufficient credit|out of credit|no credits/i;

/**
 * Call Claude through the `nimaz` AI Gateway with Unified Billing. The only
 * credential is CLOUDFLARE_AI_TOKEN — the gateway's authentication token —
 * never an Anthropic key. The Anthropic-native request (system +
 * cache_control, tools, tool_choice, model "claude-haiku-4-5") is forwarded
 * unchanged.
 */
export async function callClaude(
  request: AnthropicMessagesRequest,
  env: Env,
  metadata?: Record<string, string | number | boolean>,
): Promise<AnthropicResponse> {
  // Tolerate a secret pasted with a "Bearer " prefix or stray whitespace.
  const token = (env.CLOUDFLARE_AI_TOKEN ?? "")
    .trim()
    .replace(/^Bearer\s+/i, "");
  const res = await fetch(ENDPOINT, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "cf-aig-authorization": `Bearer ${token}`,
      "anthropic-version": ANTHROPIC_VERSION,
      // Per-feature spend breakdown in the gateway dashboard. Never contains
      // question text.
      ...(metadata ? { "cf-aig-metadata": JSON.stringify(metadata) } : {}),
    },
    body: JSON.stringify(request),
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
