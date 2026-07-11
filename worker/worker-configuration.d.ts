// Ambient environment bindings for the Nimaz AI Worker.
// Vars come from wrangler.jsonc; secrets are set via `wrangler secret put`.

interface Env {
  // KV namespace — per-device + global daily rate-limit counters.
  NIMAZ_AI_KV: KVNamespace;

  // ── Vars (wrangler.jsonc) ────────────────────────────────────────────────
  SKIP_ATTESTATION: string; // "true" | "false"
  DAILY_DEVICE_LIMIT: string; // integer string, default "20" (verified tier)
  UNVERIFIED_DAILY_DEVICE_LIMIT: string; // integer string, default "5"
  DAILY_GLOBAL_LIMIT: string; // integer string, default "500"

  // ── Secrets (`wrangler secret put`) ──────────────────────────────────────
  // Scoped Cloudflare API token with AI Gateway Run permission. Authenticates
  // the Worker to the `nimaz` gateway (Unified Billing injects the Anthropic
  // credentials — this is never an Anthropic key).
  CLOUDFLARE_AI_TOKEN: string;
  // Full Google service-account JSON (as a single string) used to mint an
  // OAuth2 token for the Play Integrity API. Optional when SKIP_ATTESTATION.
  GOOGLE_SERVICE_ACCOUNT_JSON?: string;
}
