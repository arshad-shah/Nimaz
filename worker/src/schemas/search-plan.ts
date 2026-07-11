import { z } from "zod";

// ── search-plan: input ──────────────────────────────────────────────────────
// The app hands over retrieval to the model: given only the user's question, the
// model returns the search terms + Quran references the app should fetch from its
// LOCAL database. No passages are sent — this runs BEFORE local retrieval.

export const SearchPlanInputSchema = z.object({
  question: z.string().min(3).max(500),
});

export type SearchPlanInput = z.infer<typeof SearchPlanInputSchema>;

// ── search-plan: output ─────────────────────────────────────────────────────

export const SearchPlanOutputSchema = z.object({
  // Keyword search terms — matched by the app via substring search against
  // Quran/Hadith/Dua English text. Short terms work best (substring matching).
  terms: z.array(z.string().min(1).max(40)).min(1).max(12),
  // Directly-relevant Quran ayah references, "surah:ayah" (standard numbering).
  // Hadith/Dua are addressed by opaque local IDs the model cannot know, so it
  // only ever plans them via `terms`. Individual items are validated in the
  // capability's parseResponse (malformed refs are dropped, not rejected).
  quranRefs: z.array(z.string()).max(10),
});

export type SearchPlanOutput = z.infer<typeof SearchPlanOutputSchema>;

// JSON Schema handed to Claude's forced `submit_plan` tool. Kept in lock-step
// with SearchPlanOutputSchema so the model returns exactly our output contract.
export const SEARCH_PLAN_TOOL_JSON_SCHEMA = {
  type: "object" as const,
  properties: {
    terms: {
      type: "array",
      items: { type: "string" },
      description:
        "Up to 12 short English keywords or 2-word phrases that would match relevant Quran/Hadith/Dua passages by substring. Include synonyms and closely related concepts (e.g. for patience: patience, sabr, endurance, perseverance, hardship). Prefer single words.",
    },
    quranRefs: {
      type: "array",
      items: { type: "string" },
      description:
        "Up to 10 specific, real Quran ayah references most relevant to the question, each as 'surah:ayah' (e.g. '2:153') using standard numbering. Only well-known, real references — never invent. Omit if unsure.",
    },
  },
  required: ["terms", "quranRefs"],
} as const;
