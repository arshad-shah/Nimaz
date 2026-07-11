import type { ZodType } from "zod";

// ── Anthropic Messages API shapes (only the fields we use) ──────────────────

export interface AnthropicTool {
  name: string;
  description: string;
  input_schema: Record<string, unknown>;
}

export interface AnthropicSystemBlock {
  type: "text";
  text: string;
  cache_control?: { type: "ephemeral" };
}

export interface AnthropicMessagesRequest {
  model: string;
  max_tokens: number;
  temperature?: number;
  system?: AnthropicSystemBlock[];
  messages: Array<{ role: "user" | "assistant"; content: string }>;
  tools?: AnthropicTool[];
  tool_choice?: { type: "tool"; name: string };
}

export interface AnthropicUsage {
  input_tokens: number;
  output_tokens: number;
  cache_read_input_tokens?: number;
  cache_creation_input_tokens?: number;
}

export interface AnthropicResponse {
  id: string;
  content: Array<
    | { type: "text"; text: string }
    | { type: "tool_use"; id: string; name: string; input: unknown }
  >;
  stop_reason: string | null;
  usage: AnthropicUsage;
}

// ── Capability contract ─────────────────────────────────────────────────────
// A capability is a self-contained AI feature. Adding a new one is a single new
// file in capabilities/ plus one registry entry — see README.md.

export interface Capability<I, O> {
  id: string;
  inputSchema: ZodType<I>;
  outputSchema: ZodType<O>;
  model: string;
  maxOutputTokens: number;
  buildRequest(input: I, env: Env): AnthropicMessagesRequest;
  parseResponse(raw: AnthropicResponse, input: I): O;
}
