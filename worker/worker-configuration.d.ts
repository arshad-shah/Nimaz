// Ambient environment bindings for the Nimaz AI Worker.
// Vars come from wrangler.jsonc; secrets are set via `wrangler secret put`.

interface Env {
  // KV namespace — rate-limit counters + monthly budget tally.
  NIMAZ_AI_KV: KVNamespace;

  // ── Vars (wrangler.jsonc) ────────────────────────────────────────────────
  SKIP_ATTESTATION: string; // "true" | "false"
  DAILY_DEVICE_LIMIT: string; // integer string, default "20" (verified tier)
  UNVERIFIED_DAILY_DEVICE_LIMIT: string; // integer string, default "5"
  DAILY_GLOBAL_LIMIT: string; // integer string, default "500"
  MONTHLY_BUDGET_USD: string; // integer/decimal string, default "10"
  AI_GATEWAY_BASE_URL: string; // "" or an AI Gateway base URL

  // ── Secrets (`wrangler secret put`) ──────────────────────────────────────
  ANTHROPIC_API_KEY: string;
  // Full Google service-account JSON (as a single string) used to mint an
  // OAuth2 token for the Play Integrity API. Optional when SKIP_ATTESTATION.
  GOOGLE_SERVICE_ACCOUNT_JSON?: string;
}
