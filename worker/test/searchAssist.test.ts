import { env } from "cloudflare:test";
import { describe, expect, it } from "vitest";
import app from "../src/index";
import { searchAssist } from "../src/capabilities/search-assist";
import {
  makeAssistInput,
  makeAssistOutput,
  makeEnvelope,
  mockAnthropicToolResponse,
  stubAi,
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

describe("search-assist (integration, AI binding)", () => {
  async function invoke(deviceId: string, ai: ReturnType<typeof stubAi>) {
    return app.request(
      "/v1/invoke",
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(makeEnvelope(makeAssistInput(), { deviceId })),
      },
      { ...env, AI: ai },
    );
  }

  it("returns a validated assist result via the gateway binding", async () => {
    const ai = stubAi(mockAnthropicToolResponse(makeAssistOutput()));
    const res = await invoke("assist-dev-1", ai);
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
    // Usage is echoed for the prompt-cache smoke test.
    expect(res.headers.get("x-nimaz-usage")).toContain("input_tokens");
  });

  it("routes through the nimaz gateway with the catalog model id, capability metadata, and the native request minus `model`", async () => {
    const ai = stubAi(mockAnthropicToolResponse(makeAssistOutput()));
    const res = await invoke("assist-dev-2", ai);
    expect(res.status).toBe(200);
    expect(ai.calls).toHaveLength(1);
    const call = ai.calls[0];
    // Provider + model are selected by the Cloudflare catalog id …
    expect(call.model).toBe("anthropic/claude-haiku-4-5");
    // … routed through the Unified-Billing gateway with per-feature metadata.
    expect(call.options?.gateway?.id).toBe("nimaz");
    expect(call.options?.gateway?.metadata).toEqual({
      capability: "search-assist",
    });
    // The Anthropic-native features survive the transport unchanged.
    expect(call.input.model).toBeUndefined();
    expect(call.input.tool_choice).toEqual({
      type: "tool",
      name: "submit_result",
    });
    const system = call.input.system as Array<{
      cache_control?: { type: string };
    }>;
    expect(system[0]?.cache_control).toEqual({ type: "ephemeral" });
  });

  it("allows an out-of-scope answer with no refs", async () => {
    const ai = stubAi(
      mockAnthropicToolResponse(
        makeAssistOutput({
          answer: "I can only help with Islamic topics.",
          quranRefs: [],
          terms: ["quran", "hadith"],
          confidence: "low",
        }),
      ),
    );
    const res = await invoke("assist-dev-3", ai);
    expect(res.status).toBe(200);
    const body = (await res.json()) as { quranRefs: string[] };
    expect(body.quranRefs).toEqual([]);
  });

  it("maps a gateway failure to UPSTREAM_ERROR/502", async () => {
    const ai = stubAi(new Error("AiError: model backend returned 500"));
    const res = await invoke("assist-dev-4", ai);
    expect(res.status).toBe(502);
    expect(((await res.json()) as any).error.code).toBe("UPSTREAM_ERROR");
  });

  it("maps a normalised (non-Anthropic) response shape to UPSTREAM_ERROR/502", async () => {
    // If the gateway ever flattened the response (no content[] blocks) the
    // forced tool_use would be unreachable — surface it as an upstream error.
    const ai = stubAi({ response: "plain text", usage: {} });
    const res = await invoke("assist-dev-5", ai);
    expect(res.status).toBe(502);
    expect(((await res.json()) as any).error.code).toBe("UPSTREAM_ERROR");
  });

  it("maps a tripped gateway spend limit / exhausted credits to BUDGET_EXCEEDED/503", async () => {
    const ai = stubAi(
      new Error("You have exceeded the spend limit configured for this gateway"),
    );
    const res = await invoke("assist-dev-6", ai);
    expect(res.status).toBe(503);
    expect(((await res.json()) as any).error.code).toBe("BUDGET_EXCEEDED");
  });
});
