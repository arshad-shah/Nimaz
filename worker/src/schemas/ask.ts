import { z } from "zod";

// ── ask-with-proof: input ──────────────────────────────────────────────────

export const PassageSchema = z.object({
  // Citation ID grammar mirrors the Android side: quran:{surah}:{ayah},
  // hadith:{id}, dua:{id}. The Worker treats it as an opaque token — it only
  // ever echoes back IDs that were supplied here.
  id: z.string().min(1).max(120),
  source: z.enum(["quran", "hadith", "dua"]),
  text: z.string().min(1).max(1200),
  meta: z.string().max(300),
});

export const AskInputSchema = z
  .object({
    question: z.string().min(3).max(500),
    passages: z.array(PassageSchema).min(1).max(8),
  })
  .refine(
    (v) => v.passages.reduce((n, p) => n + p.text.length, 0) <= 8000,
    { message: "total passage text must be ≤ 8000 chars", path: ["passages"] },
  );

export type AskInput = z.infer<typeof AskInputSchema>;
export type Passage = z.infer<typeof PassageSchema>;

// ── ask-with-proof: output ──────────────────────────────────────────────────

export const AskOutputSchema = z.object({
  answer: z.string().min(1),
  citationIds: z.array(z.string()),
  confidence: z.enum(["high", "medium", "low"]),
  insufficientEvidence: z.boolean(),
});

export type AskOutput = z.infer<typeof AskOutputSchema>;

// JSON Schema handed to Claude's forced `submit_answer` tool. Kept in lock-step
// with AskOutputSchema above so the model returns exactly our output contract.
export const ASK_TOOL_JSON_SCHEMA = {
  type: "object" as const,
  properties: {
    answer: {
      type: "string",
      description:
        "The grounded answer, ≤ 150 words, in the language of the question. Describe what the sources say; never issue a religious ruling.",
    },
    citationIds: {
      type: "array",
      items: { type: "string" },
      description:
        "IDs of the passages actually used to support the answer. Only use IDs that appear in the provided passages.",
    },
    confidence: {
      type: "string",
      enum: ["high", "medium", "low"],
      description: "How well the passages support the answer.",
    },
    insufficientEvidence: {
      type: "boolean",
      description:
        "true when the provided passages do not answer the question.",
    },
  },
  required: ["answer", "citationIds", "confidence", "insufficientEvidence"],
} as const;
