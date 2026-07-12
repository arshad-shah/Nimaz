// Ambient environment bindings for the Nimaz AI Worker.
// Vars come from wrangler.jsonc; secrets are set via `wrangler secret put`.

interface Env {
  // ── Vars (wrangler.jsonc) ────────────────────────────────────────────────
  SKIP_ATTESTATION: string; // "true" | "false"

  // ── Secrets (`wrangler secret put`) ──────────────────────────────────────
  // Scoped Cloudflare API token with AI Gateway Run permission. Authenticates
  // the Worker to the `nimaz` gateway (Unified Billing injects the Anthropic
  // credentials — this is never an Anthropic key).
  CLOUDFLARE_AI_TOKEN: string;
  // Full Google service-account JSON (as a single string) used to mint an
  // OAuth2 token for the Play Integrity API. Optional when SKIP_ATTESTATION.
  GOOGLE_SERVICE_ACCOUNT_JSON?: string;
}
