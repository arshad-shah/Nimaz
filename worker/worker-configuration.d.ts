// Ambient environment bindings for the Nimaz AI Worker.
// Vars come from wrangler.jsonc; secrets are set via `wrangler secret put`.

// Structural type for the AI binding's gateway-routed `run` call. The
// workers-types `Ai` interface only types first-party `@cf/…` models, so we
// declare the shape we actually use: catalog-id model (e.g.
// "anthropic/claude-haiku-4-5") + Anthropic-native input, routed through an
// AI Gateway with Unified Billing (Cloudflare-managed provider credentials).
interface AiGatewayBinding {
  run(
    model: string,
    input: Record<string, unknown>,
    options?: {
      gateway?: {
        id: string;
        metadata?: Record<string, string | number | boolean>;
      };
    },
  ): Promise<unknown>;
}

interface Env {
  // AI binding — self-authenticating Unified Billing path to Anthropic.
  AI: AiGatewayBinding;

  // KV namespace — per-device + global daily rate-limit counters.
  NIMAZ_AI_KV: KVNamespace;

  // ── Vars (wrangler.jsonc) ────────────────────────────────────────────────
  SKIP_ATTESTATION: string; // "true" | "false"
  DAILY_DEVICE_LIMIT: string; // integer string, default "20" (verified tier)
  UNVERIFIED_DAILY_DEVICE_LIMIT: string; // integer string, default "5"
  DAILY_GLOBAL_LIMIT: string; // integer string, default "500"

  // ── Secrets (`wrangler secret put`) ──────────────────────────────────────
  // Full Google service-account JSON (as a single string) used to mint an
  // OAuth2 token for the Play Integrity API. Optional when SKIP_ATTESTATION.
  GOOGLE_SERVICE_ACCOUNT_JSON?: string;
}
