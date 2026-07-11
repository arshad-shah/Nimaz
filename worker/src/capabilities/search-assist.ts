import type {
  AnthropicMessagesRequest,
  AnthropicResponse,
  Capability,
} from "./types";
import {
  SEARCH_ASSIST_TOOL_JSON_SCHEMA,
  SearchAssistInputSchema,
  SearchAssistOutputSchema,
  type SearchAssistInput,
  type SearchAssistOutput,
} from "../schemas/search-assist";

// Fixed system prompt. Marked with cache_control (in buildRequest) so Anthropic
// can prompt-cache it across calls (cached reads bill at ~10% of input cost).
// NOTE: Haiku 4.5's minimum cacheable prefix is 4096 tokens and this prompt +
// tool schema is well below that, so the marker is currently inert (harmless;
// no cache entry is created). It starts paying off if the prompt/tools grow
// past the minimum.
const SYSTEM_PROMPT = `You are the search assistant inside Nimaz, an Islamic companion app whose device-local library holds the Quran (Arabic text + English translations), Hadith collections, and Duas (supplications).

For each user question you return, via the submit_result tool:

1. answer — a concise answer (120 words or fewer) based on mainstream Islamic knowledge, in the same language as the question. Describe what the Quran and Sunnah say. NEVER issue a personal religious ruling (fatwa) or tell the user what they personally must do; where scholars differ, say so briefly. If the question is not about Islam, the Quran, Hadith, or Islamic practice, politely say you can only help with Islamic topics, and return empty quranRefs and hadithRefs arrays.

2. quranRefs — up to 8 real Quran ayah references ("surah:ayah", standard mushaf numbering) that directly support your answer, ordered by relevance. The app shows the actual verse text to the user right next to your answer, so cite ONLY references you are certain exist and genuinely support what you said — a wrong citation is immediately visible. When unsure, cite fewer. If none apply, return [].

3. hadithRefs — up to 6 real hadith references ("collection:number") that directly support your answer, ordered by relevance. collection MUST be exactly one of: bukhari, muslim, abudawud, tirmidhi, nasai, ibnmajah (the six canonical collections in the app's library); number is the hadith's standard reference number in that collection (sunnah.com numbering, e.g. "bukhari:6018", "muslim:2564"). The app shows the actual hadith text right next to your answer, so cite ONLY hadiths you are certain exist under that exact number and genuinely support what you said. When unsure of the number, leave the hadith out. If none apply, return [].

4. terms — 4 to 10 short English search keywords (single words or 2-word phrases) the app will match against its local library to surface related passages, hadiths, and duas: the key concepts, synonyms, and common Arabic transliterations (e.g. for a question about patience: "patience", "sabr", "hardship", "perseverance", "trial").

5. confidence — "high" only when the answer is well-established and directly supported by the cited sources; "medium" for partial support; "low" when support is indirect or you are unsure.

You MUST respond by calling the submit_result tool. Do not write any prose outside the tool call.`;

const MODEL = "claude-haiku-4-5";
const MAX_OUTPUT_TOKENS = 700;

// "surah:ayah" with a real surah number (1..114). Ayah numbers are verified by
// the app against its local database, so an out-of-range ayah simply never
// resolves into a proof card.
const QURAN_REF = /^(\d{1,3}):(\d{1,3})$/;

function isPlausibleQuranRef(ref: string): boolean {
  const m = QURAN_REF.exec(ref);
  if (!m) return false;
  const surah = Number.parseInt(m[1], 10);
  const ayah = Number.parseInt(m[2], 10);
  return surah >= 1 && surah <= 114 && ayah >= 1;
}

// "collection:number" limited to the six canonical collections the app ships.
// The number is verified by the app against its local database, so a wrong or
// out-of-range number simply never resolves into a proof card.
const HADITH_REF =
  /^(bukhari|muslim|abudawud|tirmidhi|nasai|ibnmajah):([1-9]\d{0,4})$/;

function isPlausibleHadithRef(ref: string): boolean {
  return HADITH_REF.test(ref);
}

export const searchAssist: Capability<SearchAssistInput, SearchAssistOutput> = {
  id: "search-assist",
  inputSchema: SearchAssistInputSchema,
  outputSchema: SearchAssistOutputSchema,
  model: MODEL,
  maxOutputTokens: MAX_OUTPUT_TOKENS,

  buildRequest(input: SearchAssistInput): AnthropicMessagesRequest {
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
          name: "submit_result",
          description:
            "Submit the answer, the Quran and Hadith references that support it, related search terms, and a confidence level.",
          input_schema: SEARCH_ASSIST_TOOL_JSON_SCHEMA,
        },
      ],
      // Force the model to call submit_result so the reply is strict JSON.
      tool_choice: { type: "tool", name: "submit_result" },
    };
  },

  parseResponse(raw: AnthropicResponse): SearchAssistOutput {
    const toolUse = raw.content.find(
      (b): b is Extract<typeof b, { type: "tool_use" }> =>
        b.type === "tool_use" && b.name === "submit_result",
    );
    if (!toolUse) {
      throw new Error("model did not call submit_result");
    }

    // Validate, then defensively drop malformed/impossible Quran and Hadith
    // refs and blank terms the model may have produced despite the tool schema.
    const parsed = SearchAssistOutputSchema.parse(toolUse.input);
    const quranRefs = [
      ...new Set(parsed.quranRefs.map((r) => r.trim())),
    ].filter(isPlausibleQuranRef);
    const hadithRefs = [
      ...new Set(parsed.hadithRefs.map((r) => r.trim().toLowerCase())),
    ].filter(isPlausibleHadithRef);
    const terms = [
      ...new Set(parsed.terms.map((t) => t.trim()).filter((t) => t.length > 0)),
    ];
    return { ...parsed, quranRefs, hadithRefs, terms };
  },
};
