import { z } from "zod";

// ── search-assist: input ─────────────────────────────────────────────────────
// The app's single AI capability: one call per submitted question. Only the
// question text is sent — no passages, no retrieval round-trip. The model
// answers from mainstream Islamic knowledge and returns the Quran references
// that support the answer plus search terms; the app then resolves both against
// its LOCAL database (real records become the proof cards and the results list).

export const SearchAssistInputSchema = z.object({
  question: z.string().min(3).max(500),
});

export type SearchAssistInput = z.infer<typeof SearchAssistInputSchema>;

// ── search-assist: output ────────────────────────────────────────────────────

export const SearchAssistOutputSchema = z.object({
  // Concise answer (≤ ~120 words), descriptive — never a religious ruling.
  answer: z.string().min(1),
  // Quran ayah references that support the answer, "surah:ayah" (standard
  // mushaf numbering). The app resolves each against its local Quran database;
  // anything that doesn't resolve is dropped, so only real verses ever surface.
  quranRefs: z.array(z.string()).max(8),
  // Short English keywords / 2-word phrases (incl. Arabic transliterations)
  // the app runs through its local Quran/Hadith/Dua search to build the
  // related-results list.
  terms: z.array(z.string().min(1).max(40)).max(10),
  confidence: z.enum(["high", "medium", "low"]),
});

export type SearchAssistOutput = z.infer<typeof SearchAssistOutputSchema>;

// JSON Schema handed to Claude's forced `submit_result` tool. Kept in lock-step
// with SearchAssistOutputSchema so the model returns exactly our contract.
export const SEARCH_ASSIST_TOOL_JSON_SCHEMA = {
  type: "object" as const,
  properties: {
    answer: {
      type: "string",
      description:
        "Concise answer (120 words or fewer) in the language of the question. Describe what the Quran and Sunnah say; never issue a personal religious ruling.",
    },
    quranRefs: {
      type: "array",
      items: { type: "string" },
      description:
        "Up to 8 real Quran ayah references that directly support the answer, each as 'surah:ayah' (e.g. '2:153') in standard mushaf numbering, ordered by relevance. Cite only references you are certain exist and genuinely support the answer — the app displays the actual verse text beside the answer. When unsure, cite fewer. Empty array if none apply.",
    },
    terms: {
      type: "array",
      items: { type: "string" },
      description:
        "4-10 short English search keywords or 2-word phrases for finding related passages, hadiths, and duas: key concepts, synonyms, and common Arabic transliterations (e.g. 'patience', 'sabr', 'hardship').",
    },
    confidence: {
      type: "string",
      enum: ["high", "medium", "low"],
      description:
        "high = well-established and directly supported by the cited ayat; medium = partial support; low = indirect or uncertain.",
    },
  },
  required: ["answer", "quranRefs", "terms", "confidence"],
} as const;
