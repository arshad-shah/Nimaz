import { ApiError } from "./errors";

// The applicationId shipped by the Android app (app/build.gradle.kts).
export const PACKAGE_NAME = "com.arshadshah.nimaz";

const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
const PLAY_INTEGRITY_SCOPE = "https://www.googleapis.com/auth/playintegrity";

/**
 * The outcome of Play Integrity verification — the Worker's only guard.
 * Rate limiting and the monthly cost cap live in the AI Gateway (Rate
 * Limiting + Spend Limit on the `nimaz` gateway), not in Worker code.
 *
 *  - "verified":    Play Integrity confirmed the official app on a sane
 *                   device (or SKIP_ATTESTATION is on) → proceed.
 *  - "unavailable": verification could not run for a reason the *caller does
 *                   not control* — the service account is not configured, or
 *                   Google's API is unreachable/erroring → fail OPEN and
 *                   proceed, but only within a bounded run of consecutive
 *                   Google failures (see MAX_CONSECUTIVE_FAIL_OPEN).
 *                   Hard-failing every outage bricked the feature for
 *                   legitimate users whenever Play services hiccuped.
 *  - "failed":      the caller did not present a usable token, or Google
 *                   decoded it and the verdict failed (app not
 *                   Play-recognized, device integrity not met, missing or
 *                   mismatched package) → rejected with ATTESTATION_FAILED.
 *
 * A missing, short or undecodable token is "failed", NOT "unavailable": it is
 * entirely caller-controlled, so treating it as an outage let anyone spend the
 * account's AI credits by simply omitting the token.
 */
export type IntegrityResult = "verified" | "unavailable" | "failed";

/**
 * Shortest plausible Play Integrity token. Real tokens (classic and standard
 * alike) are long JWS strings — anything under this is not a truncated token,
 * it is no token at all.
 */
const MIN_TOKEN_LENGTH = 10;

/**
 * Bounded fail-open. Google-side failures (token mint failure, 5xx from
 * `decodeIntegrityToken`, network throw) still pass requests through — but
 * only for this many consecutive failures inside OUTAGE_WINDOW_MS. Past the
 * bound the Worker fails closed, so a sustained "verification is broken"
 * condition cannot be ridden as a standing bypass.
 *
 * The counter lives in the isolate (the Worker has no KV), so the bound is
 * per-isolate and best-effort — defence in depth on top of the AI Gateway's
 * rate limit and spend limit, not a precise budget. Any decode that Google
 * actually answers resets it, so recovery is automatic.
 */
export const MAX_CONSECUTIVE_FAIL_OPEN = 10;
const OUTAGE_WINDOW_MS = 5 * 60_000;

let consecutiveOutages = 0;
let lastOutageAtMs = 0;

/** Test seam: clear the per-isolate fail-open counter between cases. */
export function resetIntegrityOutageState(): void {
  consecutiveOutages = 0;
  lastOutageAtMs = 0;
}

/**
 * Record a Google-side failure and decide whether we may still fail open.
 * Failures older than OUTAGE_WINDOW_MS start a fresh run, so occasional blips
 * spread over hours never accumulate into a closed gate.
 */
function outage(nowMs: number, reason: string): IntegrityResult {
  if (nowMs - lastOutageAtMs > OUTAGE_WINDOW_MS) consecutiveOutages = 0;
  lastOutageAtMs = nowMs;
  consecutiveOutages += 1;

  if (consecutiveOutages > MAX_CONSECUTIVE_FAIL_OPEN) {
    console.warn(
      JSON.stringify({
        event: "integrity_fail_closed",
        reason,
        consecutiveOutages,
      }),
    );
    return "failed";
  }
  return "unavailable";
}

/** Google answered a decode — verification works again, end the outage run. */
function outageCleared(): void {
  consecutiveOutages = 0;
}

interface ServiceAccount {
  client_email: string;
  private_key: string;
  token_uri?: string;
}

// In-memory cache for the minted Google access token, keyed by service-account
// email. Survives for the token's lifetime within a single isolate.
interface CachedToken {
  token: string;
  expiresAtMs: number;
}
const tokenCache = new Map<string, CachedToken>();

// ── WebCrypto helpers (no external JWT dependency) ──────────────────────────

function base64UrlEncode(bytes: Uint8Array): string {
  let bin = "";
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function base64UrlEncodeString(s: string): string {
  return base64UrlEncode(new TextEncoder().encode(s));
}

// Decode a PEM PKCS#8 private key into a CryptoKey for RS256 signing.
async function importPrivateKey(pem: string): Promise<CryptoKey> {
  const body = pem
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\s+/g, "");
  const der = Uint8Array.from(atob(body), (c) => c.charCodeAt(0));
  return crypto.subtle.importKey(
    "pkcs8",
    der.buffer as ArrayBuffer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
}

/**
 * Mint (or reuse a cached) OAuth2 access token for the Play Integrity API using
 * the service account's private key — a signed JWT bearer assertion, no library.
 * `nowMs` is injected for testability.
 */
export async function getGoogleAccessToken(
  serviceAccountJson: string,
  nowMs: number,
): Promise<string> {
  const sa = JSON.parse(serviceAccountJson) as ServiceAccount;

  const cached = tokenCache.get(sa.client_email);
  if (cached && cached.expiresAtMs > nowMs + 60_000) {
    return cached.token;
  }

  const iat = Math.floor(nowMs / 1000);
  const exp = iat + 3600;
  const header = { alg: "RS256", typ: "JWT" };
  const claims = {
    iss: sa.client_email,
    scope: PLAY_INTEGRITY_SCOPE,
    aud: sa.token_uri ?? GOOGLE_TOKEN_URL,
    iat,
    exp,
  };

  const signingInput = `${base64UrlEncodeString(JSON.stringify(header))}.${base64UrlEncodeString(
    JSON.stringify(claims),
  )}`;
  const key = await importPrivateKey(sa.private_key);
  const sig = new Uint8Array(
    await crypto.subtle.sign(
      "RSASSA-PKCS1-v1_5",
      key,
      new TextEncoder().encode(signingInput),
    ),
  );
  const assertion = `${signingInput}.${base64UrlEncode(sig)}`;

  const res = await fetch(sa.token_uri ?? GOOGLE_TOKEN_URL, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!res.ok) {
    throw new ApiError(
      "UPSTREAM_ERROR",
      "Could not authenticate device verification.",
    );
  }
  const body = (await res.json()) as {
    access_token: string;
    expires_in: number;
  };
  tokenCache.set(sa.client_email, {
    token: body.access_token,
    expiresAtMs: nowMs + body.expires_in * 1000,
  });
  return body.access_token;
}

interface IntegrityVerdict {
  appIntegrity?: { appRecognitionVerdict?: string };
  deviceIntegrity?: { deviceRecognitionVerdict?: string[] };
  requestDetails?: { requestPackageName?: string };
}

/**
 * Verify a request's Play Integrity token. NEVER throws.
 *
 * Returns "failed" (→ ATTESTATION_FAILED/403) whenever the *caller* is at
 * fault: no token, a token too short to be real, a token Google refuses to
 * decode, or a decoded verdict that does not clear app + device + package.
 * Returns "unavailable" (→ fail open) only when verification could not run for
 * a reason outside the caller's control — no service account configured, or a
 * Google-side error — and then only inside the bounded window enforced by
 * `outage()`.
 */
export async function checkIntegrity(
  env: Env,
  integrityToken: string,
  nowMs: number,
): Promise<IntegrityResult> {
  if (env.SKIP_ATTESTATION === "true") {
    return "verified"; // Explicit bypass for local dev/testing.
  }

  // Deployment state, not a caller-controlled one: without credentials there
  // is nothing to verify against. Unbounded fail-open — a Worker deployed
  // without the secret is documented as running unverified (setup runbook).
  if (!env.GOOGLE_SERVICE_ACCOUNT_JSON) return "unavailable";

  // Caller-controlled, so never a reason to fail open. Anyone can omit a
  // token; letting that through billed our AI credits to the whole internet.
  if (!integrityToken || integrityToken.length < MIN_TOKEN_LENGTH) {
    return "failed";
  }

  let res: Response;
  try {
    const accessToken = await getGoogleAccessToken(
      env.GOOGLE_SERVICE_ACCOUNT_JSON,
      nowMs,
    );

    const url = `https://playintegrity.googleapis.com/v1/${PACKAGE_NAME}:decodeIntegrityToken`;
    res = await fetch(url, {
      method: "POST",
      headers: {
        authorization: `Bearer ${accessToken}`,
        "content-type": "application/json",
      },
      body: JSON.stringify({ integrity_token: integrityToken }),
    });
  } catch {
    // Token mint failed, network threw, or the service-account JSON is
    // unparseable — Google-side/config fault, bounded fail-open.
    return outage(nowMs, "request_failed");
  }

  if (!res.ok) {
    // 400 INVALID_ARGUMENT means Google looked at the token and could not
    // decrypt it — that is the caller's garbage, not an outage, and treating
    // it as one would leave "send 10 junk characters" as a bypass. Every
    // other non-2xx (401/403 bad credentials, 429, 5xx) is our side's problem.
    if (res.status === 400) {
      outageCleared(); // Google answered — the API is up, this token is junk.
      return "failed";
    }
    return outage(nowMs, `http_${res.status}`);
  }

  let payload: IntegrityVerdict | undefined;
  try {
    const decoded = (await res.json()) as {
      tokenPayloadExternal?: IntegrityVerdict;
    };
    payload = decoded.tokenPayloadExternal;
  } catch {
    return outage(nowMs, "malformed_response");
  }

  // Google answered a decode, so verification is working again regardless of
  // what this particular verdict says.
  outageCleared();

  // No requestDetails at all → not a verdict we can bind to this app. Real
  // payloads always carry it; accepting its absence made the package check
  // vacuous for anything that could shape a 200 response.
  if (!payload?.requestDetails) return "failed";

  const appVerdict = payload.appIntegrity?.appRecognitionVerdict;
  const deviceVerdicts =
    payload.deviceIntegrity?.deviceRecognitionVerdict ?? [];

  const appOk = appVerdict === "PLAY_RECOGNIZED";
  // Accept the basic tier too — MEETS_DEVICE_INTEGRITY alone rejects many
  // real, unrooted devices (custom ROMs, older Widevine states).
  const deviceOk =
    deviceVerdicts.includes("MEETS_DEVICE_INTEGRITY") ||
    deviceVerdicts.includes("MEETS_BASIC_INTEGRITY");
  const pkgOk = payload.requestDetails.requestPackageName === PACKAGE_NAME;

  return appOk && deviceOk && pkgOk ? "verified" : "failed";
}
