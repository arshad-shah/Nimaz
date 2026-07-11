import { env, fetchMock } from "cloudflare:test";
import { afterEach, beforeAll, describe, expect, it } from "vitest";
import app from "../src/index";
import { searchAssist } from "../src/capabilities/search-assist";
import {
  GATEWAY_HOST,
  GATEWAY_PATH,
  makeAssistInput,
  makeAssistOutput,
  makeEnvelope,
  mockAnthropicToolResponse,
} from "./helpers";

describe("search-assist capability (unit)", () => {
  it("caches the system prompt and forces submit_result", () => {
    const req = searchAssist.buildRequest(makeAssistInput(), env);
    expect(req.system?.[0]?.cache_control).toEqual({ type: "ephemeral" });
    expect(req.tool_choice).toEqual({ type: "tool", name: "submit_result" });
    expect(req.tools?.[0]?.name).toBe("submit_result");
    expect(req.max_tokens).toBe(700);
    expect(req.temperature).toBe(0.2);
    expect(req.model).toBe("claude-haiku-4-5");
  });

  it("drops malformed and impossible Quran refs, dedupes, keeps order", () => {
    const raw = mockAnthropicToolResponse(
      makeAssistOutput({
        quranRefs: ["2:153", "not-a-ref", "2:153", "115:1", "0:4", "39:10"],
      }),
    );
    const out = searchAssist.parseResponse(raw as any, makeAssistInput());
    expect(out.quranRefs).toEqual(["2:153", "39:10"]);
  });

  it("trims and dedupes terms, dropping blanks", () => {
    const raw = mockAnthropicToolResponse(
      makeAssistOutput({ terms: [" patience ", "sabr", "patience", "  "] }),
    );
    const out = searchAssist.parseResponse(raw as any, makeAssistInput());
    expect(out.terms).toEqual(["patience", "sabr"]);
  });

  it("throws when the model did not call the tool", () => {
    const raw = {
      id: "m",
      content: [{ type: "text", text: "here is prose" }],
      stop_reason: "end_turn",
      usage: { input_tokens: 1, output_tokens: 1 },
    };
    expect(() =>
      searchAssist.parseResponse(raw as any, makeAssistInput()),
    ).toThrow();
  });
});

describe("search-assist (integration, AI Gateway Unified Billing)", () => {
  beforeAll(() => {
    fetchMock.activate();
    fetchMock.disableNetConnect();
  });
  afterEach(() => fetchMock.assertNoPendingInterceptors());

  async function invoke(deviceId: string) {
    return app.request(
      "/v1/invoke",
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(makeEnvelope(makeAssistInput(), { deviceId })),
      },
      { ...env },
    );
  }

  it("returns a validated assist result and echoes usage", async () => {
    fetchMock
      .get(GATEWAY_HOST)
      .intercept({ path: GATEWAY_PATH, method: "POST" })
      .reply(200, mockAnthropicToolResponse(makeAssistOutput()));
    const res = await invoke("assist-dev-1");
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      answer: string;
      quranRefs: string[];
      terms: string[];
      confidence: string;
    };
    expect(body.answer.length).toBeGreaterThan(0);
    expect(body.quranRefs).toEqual(["2:153", "39:10"]);
    expect(body.terms).toContain("patience");
    expect(body.confidence).toBe("high");
    // Usage is echoed for the smoke test / ops.
    expect(res.headers.get("x-nimaz-usage")).toContain("input_tokens");
  });

  it("sends the gateway token, gateway id, metadata, catalog model id, and the native request", async () => {
    let seenBody = "";
    fetchMock
      .get(GATEWAY_HOST)
      .intercept({
        path: GATEWAY_PATH,
        method: "POST",
        headers: {
          authorization: "Bearer test-gateway-token",
          "cf-aig-authorization": "Bearer test-gateway-token",
          "cf-aig-gateway-id": "nimaz",
          "anthropic-version": "2023-06-01",
          "cf-aig-metadata": JSON.stringify({ capability: "search-assist" }),
        },
        body: (raw) => {
          seenBody = String(raw);
          return true;
        },
      })
      .reply(200, mockAnthropicToolResponse(makeAssistOutput()));
    const res = await invoke("assist-dev-2");
    expect(res.status).toBe(200);
    const sent = JSON.parse(seenBody) as Record<string, any>;
    // Provider + model selected by the Cloudflare catalog id …
    expect(sent.model).toBe("anthropic/claude-haiku-4.5");
    // … and the Anthropic-native features survive the transport unchanged.
    expect(sent.tool_choice).toEqual({ type: "tool", name: "submit_result" });
    expect(sent.system[0].cache_control).toEqual({ type: "ephemeral" });
    expect(sent.max_tokens).toBe(700);
  });

  it("allows an out-of-scope answer with no refs", async () => {
    fetchMock
      .get(GATEWAY_HOST)
      .intercept({ path: GATEWAY_PATH, method: "POST" })
      .reply(
        200,
        mockAnthropicToolResponse(
          makeAssistOutput({
            answer: "I can only help with Islamic topics.",
            quranRefs: [],
            terms: ["quran", "hadith"],
            confidence: "low",
          }),
        ),
      );
    const res = await invoke("assist-dev-3");
    expect(res.status).toBe(200);
    const body = (await res.json()) as { quranRefs: string[] };
    expect(body.quranRefs).toEqual([]);
  });

  it("maps a gateway 5xx to UPSTREAM_ERROR/502", async () => {
    fetchMock
      .get(GATEWAY_HOST)
      .intercept({ path: GATEWAY_PATH, method: "POST" })
      .reply(500, { error: "boom" });
    const res = await invoke("assist-dev-4");
    expect(res.status).toBe(502);
    expect(((await res.json()) as any).error.code).toBe("UPSTREAM_ERROR");
  });

  it("maps a normalised (non-Anthropic) response shape to UPSTREAM_ERROR/502", async () => {
    fetchMock
      .get(GATEWAY_HOST)
      .intercept({ path: GATEWAY_PATH, method: "POST" })
      .reply(200, { response: "plain text", usage: {} });
    const res = await invoke("assist-dev-5");
    expect(res.status).toBe(502);
    expect(((await res.json()) as any).error.code).toBe("UPSTREAM_ERROR");
  });

  it("maps a tripped gateway spend limit / exhausted credits to BUDGET_EXCEEDED/503", async () => {
    fetchMock
      .get(GATEWAY_HOST)
      .intercept({ path: GATEWAY_PATH, method: "POST" })
      .reply(429, {
        error: { message: "You have exceeded the spend limit for this gateway" },
      });
    const res = await invoke("assist-dev-6");
    expect(res.status).toBe(503);
    expect(((await res.json()) as any).error.code).toBe("BUDGET_EXCEEDED");
  });
});
