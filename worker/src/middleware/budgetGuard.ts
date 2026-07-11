import type { AnthropicUsage } from "../capabilities/types";
import { ApiError } from "./errors";

// Haiku 4.5 pricing (USD per million tokens):
//   input  = $1.00 / MTok
//   output = $5.00 / MTok
//   cached input reads are billed at 10% of the input rate.
// We store spend as integer MICRODOLLARS (1 USD = 1_000_000 microdollars) in KV
// to avoid float drift.
const MICRO_PER_USD = 1_000_000;
const INPUT_MICRO_PER_TOKEN = 1_000_000 / 1_000_000; // $1/MTok  -> 1 micro/token
const OUTPUT_MICRO_PER_TOKEN = 5_000_000 / 1_000_000; // $5/MTok  -> 5 micro/token
const CACHE_READ_MICRO_PER_TOKEN = INPUT_MICRO_PER_TOKEN * 0.1; // 10% of input

export function utcMonthStamp(now: Date): string {
  const y = now.getUTCFullYear();
  const m = String(now.getUTCMonth() + 1).padStart(2, "0");
  return `${y}${m}`;
}

/** Convert an Anthropic usage record into integer microdollars. */
export function usageToMicrodollars(usage: AnthropicUsage): number {
  const cacheRead = usage.cache_read_input_tokens ?? 0;
  // input_tokens from the API excludes cache reads; bill them separately at 10%.
  const billedInput = usage.input_tokens;
  const micro =
    billedInput * INPUT_MICRO_PER_TOKEN +
    usage.output_tokens * OUTPUT_MICRO_PER_TOKEN +
    cacheRead * CACHE_READ_MICRO_PER_TOKEN;
  return Math.round(micro);
}

function budgetVarMicro(env: Env): number {
  const usd = Number.parseFloat(env.MONTHLY_BUDGET_USD ?? "");
  const safe = Number.isFinite(usd) && usd > 0 ? usd : 10;
  return Math.round(safe * MICRO_PER_USD);
}

function budgetKey(now: Date): string {
  return `budget:${utcMonthStamp(now)}`;
}

async function readSpent(kv: KVNamespace, key: string): Promise<number> {
  const raw = await kv.get(key);
  const n = raw ? Number.parseInt(raw, 10) : 0;
  return Number.isFinite(n) && n > 0 ? n : 0;
}

/**
 * Pre-call gate: reject when this month's spend already meets/exceeds the
 * budget. Throws ApiError(BUDGET_EXCEEDED).
 */
export async function enforceBudget(env: Env, now: Date): Promise<void> {
  const spent = await readSpent(env.NIMAZ_AI_KV, budgetKey(now));
  if (spent >= budgetVarMicro(env)) {
    throw new ApiError(
      "BUDGET_EXCEEDED",
      "AI answers are resting for now — the monthly budget has been reached. Please try again next month.",
    );
  }
}

/**
 * Post-call accounting: add the call's cost to the month's tally.
 * NOTE: read→write is not atomic, so concurrent calls can under-count slightly.
 * Acceptable for a soft monthly cap. TTL of ~63 days lets two months coexist
 * around a rollover without unbounded key growth.
 */
export async function recordSpend(
  env: Env,
  usage: AnthropicUsage,
  now: Date,
): Promise<void> {
  const key = budgetKey(now);
  const spent = await readSpent(env.NIMAZ_AI_KV, key);
  const next = spent + usageToMicrodollars(usage);
  await env.NIMAZ_AI_KV.put(key, String(next), {
    expirationTtl: 63 * 24 * 60 * 60,
  });
}
