import { env, fetchMock } from "cloudflare:test";
import { afterEach, beforeAll, describe, expect, it } from "vitest";
import app from "../src/index";
import { askWithProof } from "../src/capabilities/ask-with-proof";
import { makeAskInput, makeEnvelope, mockAnthropicToolResponse } from "./helpers";

describe("ask-with-proof capability (unit)", () => {
  it("marks the system prompt with ephemeral cache_control and forces submit_answer", () => {
    const req = askWithProof.buildRequest(makeAskInput(), env);
    expect(req.system?.[0]?.cache_control).toEqual({ type: "ephemeral" });
    expect(req.tool_choice).toEqual({ type: "tool", name: "submit_answer" });
    expect(req.tools?.[0]?.name).toBe("submit_answer");
    expect(req.max_tokens).toBe(600);
    expect(req.temperature).toBe(0.2);
    expect(req.model).toBe("claude-haiku-4-5");
  });

  it("drops citation IDs not present in the request passages", () => {
    const input = makeAskInput();
    const raw = mockAnthropicToolResponse({
      answer: "Be patient and pray.",
      citationIds: ["quran:2:153", "hadith:9999-hallucinated"],
      confidence: "high",
      insufficientEvidence: false,
    });
    const out = askWithProof.parseResponse(raw as any, input);
    expect(out.citationIds).toEqual(["quran:2:153"]);
  });

  it("throws when the model did not call the tool", () => {
    const input = makeAskInput();
    const raw = {
      id: "m",
      content: [{ type: "text", text: "here is prose" }],
      stop_reason: "end_turn",
      usage: { input_tokens: 1, output_tokens: 1 },
    };
    expect(() => askWithProof.parseResponse(raw as any, input)).toThrow();
  });
});

describe("ask-with-proof (integration)", () => {
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
        body: JSON.stringify(makeEnvelope(makeAskInput(), { deviceId })),
      },
      { ...env },
    );
  }

  it("returns a validated grounded answer", async () => {
    fetchMock
      .get("https://api.anthropic.com")
      .intercept({ path: "/v1/messages", method: "POST" })
      .reply(
        200,
        mockAnthropicToolResponse({
          answer: "The sources describe patience as sought through prayer.",
          citationIds: ["quran:2:153"],
          confidence: "high",
          insufficientEvidence: false,
        }),
      );
    const res = await invoke("ask-dev-1");
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      answer: string;
      citationIds: string[];
      confidence: string;
      insufficientEvidence: boolean;
    };
    expect(body.citationIds).toEqual(["quran:2:153"]);
    expect(body.confidence).toBe("high");
    expect(body.insufficientEvidence).toBe(false);
  });

  it("supports the insufficientEvidence path", async () => {
    fetchMock
      .get("https://api.anthropic.com")
      .intercept({ path: "/v1/messages", method: "POST" })
      .reply(
        200,
        mockAnthropicToolResponse({
          answer: "The provided sources do not address this question.",
          citationIds: [],
          confidence: "low",
          insufficientEvidence: true,
        }),
      );
    const res = await invoke("ask-dev-2");
    expect(res.status).toBe(200);
    const body = (await res.json()) as { insufficientEvidence: boolean };
    expect(body.insufficientEvidence).toBe(true);
  });

  it("maps an Anthropic 5xx to UPSTREAM_ERROR/502", async () => {
    fetchMock
      .get("https://api.anthropic.com")
      .intercept({ path: "/v1/messages", method: "POST" })
      .reply(500, { error: "boom" });
    const res = await invoke("ask-dev-3");
    expect(res.status).toBe(502);
    expect(((await res.json()) as any).error.code).toBe("UPSTREAM_ERROR");
  });
});
