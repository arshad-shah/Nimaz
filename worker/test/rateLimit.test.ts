import { env } from "cloudflare:test";
import { describe, expect, it } from "vitest";
import app from "../src/index";
import {
  secondsUntilUtcMidnight,
  utcDayStamp,
} from "../src/middleware/rateLimit";
import {
  makeAssistInput,
  makeAssistOutput,
  makeEnvelope,
  mockAnthropicToolResponse,
  stubAi,
} from "./helpers";

describe("UTC helpers", () => {
  it("formats the UTC day stamp", () => {
    expect(utcDayStamp(new Date("2026-07-11T23:59:00Z"))).toBe("20260711");
    expect(utcDayStamp(new Date("2026-01-05T00:00:00Z"))).toBe("20260105");
  });

  it("computes seconds until the next UTC midnight", () => {
    const s = secondsUntilUtcMidnight(new Date("2026-07-11T23:00:00Z"));
    expect(s).toBe(3600);
  });
});

describe("rate limiting (integration)", () => {
  async function invoke(deviceId: string, overrideEnv: Partial<Env> = {}) {
    return app.request(
      "/v1/invoke",
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(makeEnvelope(makeAssistInput(), { deviceId })),
      },
      {
        ...env,
        AI: stubAi(mockAnthropicToolResponse(makeAssistOutput())),
        DAILY_DEVICE_LIMIT: "3",
        DAILY_GLOBAL_LIMIT: "100",
        ...overrideEnv,
      },
    );
  }

  it("allows up to the device cap then returns 429 with retryAfterSeconds", async () => {
    // Cap is 3 → first three succeed, the fourth is rate limited.
    const dev = "rl-device-A";
    for (let i = 0; i < 3; i++) {
      const ok = await invoke(dev);
      expect(ok.status).toBe(200);
    }
    const limited = await invoke(dev);
    expect(limited.status).toBe(429);
    const body = (await limited.json()) as {
      error: { code: string; retryAfterSeconds: number };
    };
    expect(body.error.code).toBe("RATE_LIMITED");
    expect(body.error.retryAfterSeconds).toBeGreaterThan(0);
  });

  it("applies the smaller unverified cap when attestation is unavailable", async () => {
    // SKIP_ATTESTATION off + no service account → every request classifies as
    // unverified → UNVERIFIED_DAILY_DEVICE_LIMIT (2) applies instead of 3.
    const unverifiedEnv: Partial<Env> = {
      SKIP_ATTESTATION: "false",
      GOOGLE_SERVICE_ACCOUNT_JSON: undefined,
      UNVERIFIED_DAILY_DEVICE_LIMIT: "2",
    };
    const dev = "rl-device-unverified";
    for (let i = 0; i < 2; i++) {
      const ok = await invoke(dev, unverifiedEnv);
      expect(ok.status).toBe(200);
    }
    const limited = await invoke(dev, unverifiedEnv);
    expect(limited.status).toBe(429);
    expect(((await limited.json()) as any).error.code).toBe("RATE_LIMITED");
  });
});
