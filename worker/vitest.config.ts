import { defineWorkersConfig } from "@cloudflare/vitest-pool-workers/config";

export default defineWorkersConfig({
  test: {
    poolOptions: {
      workers: {
        // Test-time miniflare config. Bindings here are test values so tests
        // never touch real credentials or the real KV namespace. Keep
        // compatibility settings in sync with wrangler.jsonc.
        miniflare: {
          compatibilityDate: "2024-12-30",
          compatibilityFlags: ["nodejs_compat"],
          kvNamespaces: ["NIMAZ_AI_KV"],
          bindings: {
            SKIP_ATTESTATION: "true",
            DAILY_DEVICE_LIMIT: "3",
            UNVERIFIED_DAILY_DEVICE_LIMIT: "2",
            DAILY_GLOBAL_LIMIT: "5",
            CLOUDFLARE_AI_TOKEN: "test-gateway-token",
          },
        },
      },
    },
  },
});
