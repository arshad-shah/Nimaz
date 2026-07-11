import { env } from "cloudflare:test";
import { describe, expect, it } from "vitest";
import app from "../src/index";
import { makeAskInput, makeEnvelope } from "./helpers";

async function invoke(body: unknown, overrideEnv: Partial<Env> = {}) {
  return app.request(
    "/v1/invoke",
    {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(body),
    },
    { ...env, ...overrideEnv },
  );
}

describe("health", () => {
  it("lists registered capabilities without auth", async () => {
    const res = await app.request("/v1/health", {}, env);
    expect(res.status).toBe(200);
    const body = (await res.json()) as { ok: boolean; capabilities: string[] };
    expect(body.ok).toBe(true);
    expect(body.capabilities).toContain("ask-with-proof");
  });
});

describe("dispatch validation", () => {
  it("rejects a non-JSON body with INVALID_INPUT/400", async () => {
    const res = await app.request(
      "/v1/invoke",
      { method: "POST", body: "not json" },
      env,
    );
    expect(res.status).toBe(400);
    const body = (await res.json()) as { error: { code: string } };
    expect(body.error.code).toBe("INVALID_INPUT");
  });

  it("rejects a malformed envelope (missing deviceId)", async () => {
    const res = await invoke({ capability: "ask-with-proof", input: {} });
    expect(res.status).toBe(400);
    expect(((await res.json()) as any).error.code).toBe("INVALID_INPUT");
  });

  it("rejects an unknown capability", async () => {
    const res = await invoke(makeEnvelope(makeAskInput(), { capability: "does-not-exist" }));
    expect(res.status).toBe(400);
    const body = (await res.json()) as { error: { code: string; message: string } };
    expect(body.error.code).toBe("INVALID_INPUT");
    expect(body.error.message).toContain("does-not-exist");
  });

  it("rejects capability input that fails the Zod schema (question too short)", async () => {
    const res = await invoke(makeEnvelope(makeAskInput({ question: "hi" })));
    expect(res.status).toBe(400);
    expect(((await res.json()) as any).error.code).toBe("INVALID_INPUT");
  });

  it("rejects when total passage chars exceed 8000", async () => {
    const big = "x".repeat(1100);
    const passages = Array.from({ length: 8 }, (_, i) => ({
      id: `quran:2:${i + 1}`,
      source: "quran" as const,
      text: big,
      meta: "meta",
    }));
    const res = await invoke(makeEnvelope(makeAskInput({ passages })));
    expect(res.status).toBe(400);
  });
});
