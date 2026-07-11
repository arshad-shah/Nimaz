import type {
  AnthropicMessagesRequest,
  AnthropicResponse,
} from "../capabilities/types";

const ANTHROPIC_VERSION = "2023-06-01";
const DEFAULT_BASE_URL = "https://api.anthropic.com";

export class UpstreamError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = "UpstreamError";
  }
}

// Resolve the Messages endpoint. When AI_GATEWAY_BASE_URL is set we route
// through the Cloudflare AI Gateway (adds caching/analytics/rate-limiting);
// otherwise we call Anthropic directly.
function resolveEndpoint(env: Env): string {
  const gateway = env.AI_GATEWAY_BASE_URL?.trim();
  const base = gateway && gateway.length > 0 ? gateway : DEFAULT_BASE_URL;
  return `${base.replace(/\/$/, "")}/v1/messages`;
}

export async function callClaude(
  request: AnthropicMessagesRequest,
  env: Env,
): Promise<AnthropicResponse> {
  const res = await fetch(resolveEndpoint(env), {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-api-key": env.ANTHROPIC_API_KEY,
      "anthropic-version": ANTHROPIC_VERSION,
    },
    body: JSON.stringify(request),
  });

  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new UpstreamError(
      `Anthropic API returned ${res.status}: ${body.slice(0, 500)}`,
      res.status,
    );
  }

  return (await res.json()) as AnthropicResponse;
}
