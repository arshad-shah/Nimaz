import type {
  AnthropicMessagesRequest,
  AnthropicResponse,
  Capability,
} from "./types";
import {
  SEARCH_PLAN_TOOL_JSON_SCHEMA,
  SearchPlanInputSchema,
  SearchPlanOutputSchema,
  type SearchPlanInput,
  type SearchPlanOutput,
} from "../schemas/search-plan";

// Fixed system prompt (cache_control ephemeral, like ask-with-proof) — it never
// changes, so after the first call it is read from prompt cache at ~10% cost.
const SYSTEM_PROMPT = `You plan a search over a LOCAL Islamic content database (Quran translations in English, Hadith collections, and Dua/supplication collections). You do NOT answer the question — you only decide what the app should retrieve.

Given the user's question, return:
- terms: up to 12 short English search keywords (single words or 2-word phrases). The app matches these by substring against the English text, so keep them short and include synonyms and closely related concepts. Example: for "how do I cope with grief?" → ["grief", "patience", "sabr", "hardship", "loss", "death", "comfort", "mourning", "trust in Allah"].
- quranRefs: up to 10 specific Quran ayah references most directly relevant, each as "surah:ayah" using standard mushaf numbering (e.g. "2:153"). Only include real, well-known references. If you are not confident a reference exists, leave it out — never invent references. You cannot know Hadith or Dua database IDs, so plan those only through terms.

You MUST respond by calling the submit_plan tool. Do not write any prose outside the tool call.`;

const MODEL = "claude-haiku-4-5";
const MAX_OUTPUT_TOKENS = 300;

export const searchPlan: Capability<SearchPlanInput, SearchPlanOutput> = {
  id: "search-plan",
  inputSchema: SearchPlanInputSchema,
  outputSchema: SearchPlanOutputSchema,
  model: MODEL,
  maxOutputTokens: MAX_OUTPUT_TOKENS,

  buildRequest(input: SearchPlanInput): AnthropicMessagesRequest {
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
      messages: [{ role: "user", content: `Question:\n${input.question}` }],
      tools: [
        {
          name: "submit_plan",
          description:
            "Submit the search terms and Quran references the app should retrieve to answer the question.",
          input_schema: SEARCH_PLAN_TOOL_JSON_SCHEMA,
        },
      ],
      // Force the model to call submit_plan so the reply is strict JSON.
      tool_choice: { type: "tool", name: "submit_plan" },
    };
  },

  parseResponse(raw: AnthropicResponse): SearchPlanOutput {
    const toolUse = raw.content.find(
      (b): b is Extract<typeof b, { type: "tool_use" }> =>
        b.type === "tool_use" && b.name === "submit_plan",
    );
    if (!toolUse) {
      throw new Error("model did not call submit_plan");
    }

    // Validate, then defensively drop any malformed Quran refs the model may
    // have produced despite the tool schema (keeps the output contract clean).
    const parsed = SearchPlanOutputSchema.parse(toolUse.input);
    const quranRefs = parsed.quranRefs.filter((r) => /^\d{1,3}:\d{1,3}$/.test(r));
    return { ...parsed, quranRefs };
  },
};
