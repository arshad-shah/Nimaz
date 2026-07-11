import { ApiError } from "./errors";
import type { IntegrityTier } from "./integrity";

// UTC calendar helpers. We avoid Date.now() semantics that differ per timezone:
// all keys and rollovers are UTC so a "day" is unambiguous across devices.

export function utcDayStamp(now: Date): string {
  const y = now.getUTCFullYear();
  const m = String(now.getUTCMonth() + 1).padStart(2, "0");
  const d = String(now.getUTCDate()).padStart(2, "0");
  return `${y}${m}${d}`;
}

/** Seconds from `now` until the next UTC midnight. */
export function secondsUntilUtcMidnight(now: Date): number {
  const next = Date.UTC(
    now.getUTCFullYear(),
    now.getUTCMonth(),
    now.getUTCDate() + 1,
    0,
    0,
    0,
    0,
  );
  return Math.max(1, Math.ceil((next - now.getTime()) / 1000));
}

function intVar(raw: string | undefined, fallback: number): number {
  const n = Number.parseInt(raw ?? "", 10);
  return Number.isFinite(n) && n > 0 ? n : fallback;
}

async function incrementCounter(
  kv: KVNamespace,
  key: string,
  ttlSeconds: number,
): Promise<number> {
  // NOTE: KV reads are eventually consistent and this read→write is not atomic,
  // so under bursty concurrent traffic a device could exceed the cap slightly.
  // Acceptable for a per-device daily cap that only needs to be roughly right.
  const current = intVar((await kv.get(key)) ?? undefined, 0);
  const next = current + 1;
  await kv.put(key, String(next), { expirationTtl: ttlSeconds });
  return next;
}

/**
 * Enforce the per-device and global daily request caps. Increments both
 * counters; throws ApiError(RATE_LIMITED) with retryAfterSeconds when either
 * cap is exceeded.
 *
 * The per-device cap is tiered by the request's [IntegrityTier]: devices that
 * passed Play Integrity get the full DAILY_DEVICE_LIMIT; unverified requests
 * (no token, verification unavailable, failed verdict) get the much smaller
 * UNVERIFIED_DAILY_DEVICE_LIMIT instead of being blocked outright.
 */
export async function enforceRateLimit(
  env: Env,
  deviceId: string,
  now: Date,
  tier: IntegrityTier = "verified",
): Promise<void> {
  const day = utcDayStamp(now);
  const deviceLimit =
    tier === "verified"
      ? intVar(env.DAILY_DEVICE_LIMIT, 20)
      : intVar(env.UNVERIFIED_DAILY_DEVICE_LIMIT, 5);
  const globalLimit = intVar(env.DAILY_GLOBAL_LIMIT, 500);
  const retryAfter = secondsUntilUtcMidnight(now);

  // Per-device: 25h TTL so the key survives a full day plus clock skew.
  const deviceKey = `rl:${deviceId}:${day}`;
  const deviceCount = await incrementCounter(
    env.NIMAZ_AI_KV,
    deviceKey,
    25 * 60 * 60,
  );
  if (deviceCount > deviceLimit) {
    throw new ApiError(
      "RATE_LIMITED",
      "Daily question limit reached for this device. Try again tomorrow.",
      retryAfter,
    );
  }

  const globalKey = `rl:global:${day}`;
  const globalCount = await incrementCounter(
    env.NIMAZ_AI_KV,
    globalKey,
    25 * 60 * 60,
  );
  if (globalCount > globalLimit) {
    throw new ApiError(
      "RATE_LIMITED",
      "The service is busy right now. Please try again later.",
      retryAfter,
    );
  }
}
