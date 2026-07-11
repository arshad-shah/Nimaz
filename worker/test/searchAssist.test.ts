import { env, fetchMock } from "cloudflare:test";
import { afterEach, beforeAll, describe, expect, it } from "vitest";
import app from "../src/index";
import { searchAssist } from "../src/capabilities/search-assist";
import {
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

describe("search-assist (integration)", () => {
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

  it("returns a validated assist result", async () => {
    fetchMock
      .get("https://api.anthropic.com")
      .intercept({ path: "/v1/messages", method: "POST" })
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
  });

  it("allows an out-of-scope answer with no refs", async () => {
    fetchMock
      .get("https://api.anthropic.com")
      .intercept({ path: "/v1/messages", method: "POST" })
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
    const res = await invoke("assist-dev-2");
    expect(res.status).toBe(200);
    const body = (await res.json()) as { quranRefs: string[] };
    expect(body.quranRefs).toEqual([]);
  });

  it("maps an Anthropic 5xx to UPSTREAM_ERROR/502", async () => {
    fetchMock
      .get("https://api.anthropic.com")
      .intercept({ path: "/v1/messages", method: "POST" })
      .reply(500, { error: "boom" });
    const res = await invoke("assist-dev-3");
    expect(res.status).toBe(502);
    expect(((await res.json()) as any).error.code).toBe("UPSTREAM_ERROR");
  });
});
