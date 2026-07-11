import { defineWorkersConfig } from "@cloudflare/vitest-pool-workers/config";

export default defineWorkersConfig({
  test: {
    poolOptions: {
      workers: {
        // Miniflare is configured inline (NOT from wrangler.jsonc): the `ai`
        // binding declared there is a real Cloudflare service that miniflare
        // cannot emulate, so pointing the pool at the config file breaks
        // startup. Tests inject a stubbed `AI` binding per request instead
        // (see test/helpers.ts stubAi), and everything else the Worker needs
        // is declared here. Keep compatibility settings in sync with
        // wrangler.jsonc.
        miniflare: {
          compatibilityDate: "2024-12-30",
          compatibilityFlags: ["nodejs_compat"],
          kvNamespaces: ["NIMAZ_AI_KV"],
          bindings: {
            SKIP_ATTESTATION: "true",
            DAILY_DEVICE_LIMIT: "3",
            UNVERIFIED_DAILY_DEVICE_LIMIT: "2",
            DAILY_GLOBAL_LIMIT: "5",
          },
        },
      },
    },
  },
});
