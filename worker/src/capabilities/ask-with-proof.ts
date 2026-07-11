import type {
  AnthropicMessagesRequest,
  AnthropicResponse,
  Capability,
} from "./types";
import {
  ASK_TOOL_JSON_SCHEMA,
  AskInputSchema,
  AskOutputSchema,
  type AskInput,
  type AskOutput,
} from "../schemas/ask";

// Fixed system prompt. Marked with cache_control (in buildRequest) so Anthropic
// prompt-caches it across calls — it never changes, so every call after the
// first reads it from cache at ~10% of input cost.
const SYSTEM_PROMPT = `You are a careful assistant that answers questions about Islam using ONLY the passages provided by the user (excerpts from the Quran, Hadith, and Dua collections).

Rules you must always follow:
- Answer ONLY from the provided passages. Do not use outside knowledge.
- If the passages do not answer the question, set insufficientEvidence to true and say plainly that the provided sources do not contain the answer.
- Never issue a religious ruling (fatwa) or tell the user what they must do. Describe only what the sources say.
- Cite only IDs that appear in the provided passages, in citationIds. Do not invent IDs.
- Keep the answer to 150 words or fewer.
- Answer in the same language as the question (English, or the language of the passage translations).
- Set confidence to "high" only when the passages directly and clearly answer the question; "medium" for partial support; "low" for weak or tangential support.

You MUST respond by calling the submit_answer tool. Do not write any prose outside the tool call.`;

const MODEL = "claude-haiku-4-5";
const MAX_OUTPUT_TOKENS = 600;

function buildUserMessage(input: AskInput): string {
  const passages = input.passages
    .map(
      (p) =>
        `[id: ${p.id}] (source: ${p.source}; ${p.meta})\n${p.text}`,
    )
    .join("\n\n");
  return `Question:\n${input.question}\n\nPassages:\n${passages}`;
}

export const askWithProof: Capability<AskInput, AskOutput> = {
  id: "ask-with-proof",
  inputSchema: AskInputSchema,
  outputSchema: AskOutputSchema,
  model: MODEL,
  maxOutputTokens: MAX_OUTPUT_TOKENS,

  buildRequest(input: AskInput): AnthropicMessagesRequest {
    return {
      model: MODEL,
      max_tokens: MAX_OUTPUT_TOKENS,
      temperature: 0.2,
      system: [
        {
          type: "text",
          text: SYSTEM_PROMPT,
          cache_control: { type: "ephemeral" },
        },
      ],
      messages: [{ role: "user", content: buildUserMessage(input) }],
      tools: [
        {
          name: "submit_answer",
          description:
            "Submit the grounded answer, its supporting citation IDs, a confidence level, and whether the evidence was insufficient.",
          input_schema: ASK_TOOL_JSON_SCHEMA,
        },
      ],
      // Force the model to call submit_answer so the reply is strict JSON.
      tool_choice: { type: "tool", name: "submit_answer" },
    };
  },

  parseResponse(raw: AnthropicResponse, input: AskInput): AskOutput {
    const toolUse = raw.content.find(
      (b): b is Extract<typeof b, { type: "tool_use" }> =>
        b.type === "tool_use" && b.name === "submit_answer",
    );
    if (!toolUse) {
      throw new Error("model did not call submit_answer");
    }

    // Validate the tool input against the strict output contract.
    const parsed = AskOutputSchema.parse(toolUse.input);

    // Defensive post-processing: drop any citationId the model produced that is
    // not one of the IDs we actually sent. Guards against hallucinated citations.
    const allowed = new Set(input.passages.map((p) => p.id));
    const citationIds = parsed.citationIds.filter((id) => allowed.has(id));

    return { ...parsed, citationIds };
  },
};
