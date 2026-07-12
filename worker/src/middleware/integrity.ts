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
 *  - "unavailable": verification could not run — missing token, service
 *                   account not configured, Google outage → fail OPEN and
 *                   proceed. Hard-failing these paths bricked the feature for
 *                   legitimate users whenever Play services hiccuped; the
 *                   gateway's rate limit + spend limit bound the abuse cost.
 *  - "failed":      Google decoded the token and the verdict failed (app not
 *                   Play-recognized, device integrity not met, package
 *                   mismatch) → the request is rejected (ATTESTATION_FAILED).
 */
export type IntegrityResult = "verified" | "unavailable" | "failed";

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
 * Verify a request's Play Integrity token. NEVER throws. Only an explicit
 * failed verdict returns "failed" (and blocks the request); every path where
 * verification cannot run — missing token, unconfigured service account,
 * Google API outage — returns "unavailable" and the request proceeds.
 */
export async function checkIntegrity(
  env: Env,
  integrityToken: string,
  nowMs: number,
): Promise<IntegrityResult> {
  if (env.SKIP_ATTESTATION === "true") {
    return "verified"; // Explicit bypass for local dev/testing.
  }

  // Not configured yet, or the app couldn't obtain a token (Play services
  // unavailable, offline fetch) → verification can't run; fail open.
  if (!env.GOOGLE_SERVICE_ACCOUNT_JSON) return "unavailable";
  if (!integrityToken || integrityToken.length < 10) return "unavailable";

  try {
    const accessToken = await getGoogleAccessToken(
      env.GOOGLE_SERVICE_ACCOUNT_JSON,
      nowMs,
    );

    const url = `https://playintegrity.googleapis.com/v1/${PACKAGE_NAME}:decodeIntegrityToken`;
    const res = await fetch(url, {
      method: "POST",
      headers: {
        authorization: `Bearer ${accessToken}`,
        "content-type": "application/json",
      },
      body: JSON.stringify({ integrity_token: integrityToken }),
    });
    if (!res.ok) return "unavailable";

    const decoded = (await res.json()) as {
      tokenPayloadExternal?: IntegrityVerdict;
    };
    const payload = decoded.tokenPayloadExternal;

    const appVerdict = payload?.appIntegrity?.appRecognitionVerdict;
    const deviceVerdicts =
      payload?.deviceIntegrity?.deviceRecognitionVerdict ?? [];
    const pkg = payload?.requestDetails?.requestPackageName;

    const appOk = appVerdict === "PLAY_RECOGNIZED";
    // Accept the basic tier too — MEETS_DEVICE_INTEGRITY alone rejects many
    // real, unrooted devices (custom ROMs, older Widevine states).
    const deviceOk =
      deviceVerdicts.includes("MEETS_DEVICE_INTEGRITY") ||
      deviceVerdicts.includes("MEETS_BASIC_INTEGRITY");
    const pkgOk = pkg === undefined || pkg === PACKAGE_NAME;

    return appOk && deviceOk && pkgOk ? "verified" : "failed";
  } catch {
    // Google outage / transient failure — never punish the user for it.
    return "unavailable";
  }
}
