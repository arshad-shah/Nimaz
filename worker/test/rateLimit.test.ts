import { env, fetchMock } from "cloudflare:test";
import { afterEach, beforeAll, describe, expect, it } from "vitest";
import app from "../src/index";
import {
  secondsUntilUtcMidnight,
  utcDayStamp,
} from "../src/middleware/rateLimit";
import { makeAskInput, makeEnvelope, mockAnthropicToolResponse } from "./helpers";

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
  beforeAll(() => {
    fetchMock.activate();
    fetchMock.disableNetConnect();
  });
  afterEach(() => fetchMock.assertNoPendingInterceptors());

  function stubAnthropic(times: number) {
    for (let i = 0; i < times; i++) {
      fetchMock
        .get("https://api.anthropic.com")
        .intercept({ path: "/v1/messages", method: "POST" })
        .reply(
          200,
          mockAnthropicToolResponse({
            answer: "Be patient.",
            citationIds: ["quran:2:153"],
            confidence: "high",
            insufficientEvidence: false,
          }),
        );
    }
  }

  async function invoke(deviceId: string) {
    return app.request(
      "/v1/invoke",
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(
          makeEnvelope(makeAskInput(), { deviceId }),
        ),
      },
      { ...env, DAILY_DEVICE_LIMIT: "3", DAILY_GLOBAL_LIMIT: "100" },
    );
  }

  it("allows up to the device cap then returns 429 with retryAfterSeconds", async () => {
    // Cap is 3 → first three succeed, the fourth is rate limited.
    stubAnthropic(3);
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
});
