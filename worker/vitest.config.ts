import { defineWorkersConfig } from "@cloudflare/vitest-pool-workers/config";

export default defineWorkersConfig({
  test: {
    poolOptions: {
      workers: {
        wrangler: { configPath: "./wrangler.jsonc" },
        miniflare: {
          // Test-time bindings. Secrets/vars here override wrangler.jsonc so
          // tests never touch real credentials or the real KV namespace.
          kvNamespaces: ["NIMAZ_AI_KV"],
          bindings: {
            SKIP_ATTESTATION: "true",
            DAILY_DEVICE_LIMIT: "3",
            DAILY_GLOBAL_LIMIT: "5",
            MONTHLY_BUDGET_USD: "10",
            AI_GATEWAY_BASE_URL: "",
            ANTHROPIC_API_KEY: "test-key",
          },
        },
      },
    },
  },
});
