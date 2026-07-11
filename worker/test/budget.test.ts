import { env, fetchMock } from "cloudflare:test";
import { afterEach, beforeAll, describe, expect, it } from "vitest";
import app from "../src/index";
import {
  usageToMicrodollars,
  utcMonthStamp,
} from "../src/middleware/budgetGuard";
import {
  makeAssistInput,
  makeAssistOutput,
  makeEnvelope,
  mockAnthropicToolResponse,
} from "./helpers";

describe("budget math", () => {
  it("prices input at $1/MTok and output at $5/MTok (microdollars)", () => {
    // 1_000_000 input tokens = $1 = 1_000_000 micro
    expect(
      usageToMicrodollars({ input_tokens: 1_000_000, output_tokens: 0 }),
    ).toBe(1_000_000);
    // 1_000_000 output tokens = $5 = 5_000_000 micro
    expect(
      usageToMicrodollars({ input_tokens: 0, output_tokens: 1_000_000 }),
    ).toBe(5_000_000);
  });

  it("bills cache-read tokens at 10% of the input rate", () => {
    // 1_000_000 cache reads at 10% of $1/MTok = $0.10 = 100_000 micro
    expect(
      usageToMicrodollars({
        input_tokens: 0,
        output_tokens: 0,
        cache_read_input_tokens: 1_000_000,
      }),
    ).toBe(100_000);
  });

  it("stamps the UTC month", () => {
    expect(utcMonthStamp(new Date("2026-07-11T00:00:00Z"))).toBe("202607");
  });
});

describe("budget guard (integration)", () => {
  beforeAll(() => {
    fetchMock.activate();
    fetchMock.disableNetConnect();
  });
  afterEach(() => fetchMock.assertNoPendingInterceptors());

  it("returns 503 BUDGET_EXCEEDED when the month's spend meets the cap", async () => {
    // Pre-seed the month's budget key at/over the cap so the pre-call gate trips.
    const key = `budget:${utcMonthStamp(new Date())}`;
    // MONTHLY_BUDGET_USD "0.000001" -> 1 microdollar cap; seed 1 micro.
    await env.NIMAZ_AI_KV.put(key, "1");
    const res = await app.request(
      "/v1/invoke",
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(makeEnvelope(makeAssistInput(), { deviceId: "budget-dev" })),
      },
      { ...env, MONTHLY_BUDGET_USD: "0.000001" },
    );
    expect(res.status).toBe(503);
    const body = (await res.json()) as { error: { code: string } };
    expect(body.error.code).toBe("BUDGET_EXCEEDED");
    // No Anthropic call should have been made (no interceptor registered).
  });

  it("records spend after a successful call", async () => {
    fetchMock
      .get("https://api.anthropic.com")
      .intercept({ path: "/v1/messages", method: "POST" })
      .reply(
        200,
        mockAnthropicToolResponse(makeAssistOutput(), {
          input_tokens: 1_000_000,
          output_tokens: 0,
        }),
      );
    const res = await app.request(
      "/v1/invoke",
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(makeEnvelope(makeAssistInput(), { deviceId: "spend-dev" })),
      },
      { ...env },
    );
    expect(res.status).toBe(200);
    const key = `budget:${utcMonthStamp(new Date())}`;
    const stored = await env.NIMAZ_AI_KV.get(key);
    expect(stored).toBe("1000000"); // $1 recorded
  });
});
