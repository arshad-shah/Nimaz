import { env, fetchMock } from "cloudflare:test";
import { afterEach, beforeAll, describe, expect, it } from "vitest";
import app from "../src/index";
import { searchPlan } from "../src/capabilities/search-plan";
import { makePlanEnvelope, mockAnthropicToolResponse } from "./helpers";

describe("search-plan capability (unit)", () => {
  it("caches the system prompt and forces submit_plan", () => {
    const req = searchPlan.buildRequest({ question: "What is patience?" }, env);
    expect(req.system?.[0]?.cache_control).toEqual({ type: "ephemeral" });
    expect(req.tool_choice).toEqual({ type: "tool", name: "submit_plan" });
    expect(req.tools?.[0]?.name).toBe("submit_plan");
    expect(req.model).toBe("claude-haiku-4-5");
  });

  it("drops malformed Quran refs the model returns", () => {
    const raw = mockAnthropicToolResponse(
      { terms: ["patience", "sabr"], quranRefs: ["2:153", "not-a-ref", "39:10"] },
      undefined,
      "submit_plan",
    );
    const out = searchPlan.parseResponse(raw as any, { question: "q" });
    expect(out.terms).toEqual(["patience", "sabr"]);
    expect(out.quranRefs).toEqual(["2:153", "39:10"]);
  });

  it("throws when the model did not call the tool", () => {
    const raw = {
      id: "m",
      content: [{ type: "text", text: "here is prose" }],
      stop_reason: "end_turn",
      usage: { input_tokens: 1, output_tokens: 1 },
    };
    expect(() => searchPlan.parseResponse(raw as any, { question: "q" })).toThrow();
  });
});

describe("search-plan (integration)", () => {
  beforeAll(() => {
    fetchMock.activate();
    fetchMock.disableNetConnect();
  });
  afterEach(() => fetchMock.assertNoPendingInterceptors());

  it("returns a validated plan", async () => {
    fetchMock
      .get("https://api.anthropic.com")
      .intercept({ path: "/v1/messages", method: "POST" })
      .reply(
        200,
        mockAnthropicToolResponse(
          { terms: ["grief", "patience", "sabr"], quranRefs: ["2:153"] },
          undefined,
          "submit_plan",
        ),
      );
    const res = await app.request(
      "/v1/invoke",
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(makePlanEnvelope("How do I cope with grief?")),
      },
      { ...env },
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as { terms: string[]; quranRefs: string[] };
    expect(body.terms).toContain("patience");
    expect(body.quranRefs).toEqual(["2:153"]);
  });
});
