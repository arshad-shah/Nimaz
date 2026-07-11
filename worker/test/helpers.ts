import type { SearchAssistInput } from "../src/schemas/search-assist";

export function makeAssistInput(
  overrides: Partial<SearchAssistInput> = {},
): SearchAssistInput {
  return {
    question: "What does the Quran say about patience?",
    ...overrides,
  };
}

/** A well-formed search-assist tool output the mock Anthropic API returns. */
export function makeAssistOutput(overrides: Record<string, unknown> = {}) {
  return {
    answer:
      "The Quran repeatedly encourages patience (sabr), promising that Allah is with the patient.",
    quranRefs: ["2:153", "39:10"],
    terms: ["patience", "sabr", "hardship", "perseverance"],
    confidence: "high",
    ...overrides,
  };
}

export function makeEnvelope(
  input: unknown,
  extra: Record<string, unknown> = {},
) {
  return {
    capability: "search-assist",
    integrityToken: "test-integrity-token",
    deviceId: "test-device-0001",
    input,
    ...extra,
  };
}

/** A well-formed Anthropic Messages response that calls the named tool. */
export function mockAnthropicToolResponse(
  toolInput: unknown,
  usage: {
    input_tokens: number;
    output_tokens: number;
    cache_read_input_tokens?: number;
  } = {
    input_tokens: 500,
    output_tokens: 120,
  },
  toolName = "submit_result",
) {
  return {
    id: "msg_test",
    content: [
      {
        type: "tool_use",
        id: "toolu_test",
        name: toolName,
        input: toolInput,
      },
    ],
    stop_reason: "tool_use",
    usage,
  };
}

export interface AiStubCall {
  model: string;
  input: Record<string, unknown>;
  options?: {
    gateway?: {
      id: string;
      metadata?: Record<string, string | number | boolean>;
    };
  };
}

/**
 * Stub for the AI binding (`env.AI`). Pass the value each `run` resolves with,
 * or an Error to make it reject. Recorded calls are exposed on `.calls` so
 * tests can assert the catalog model id, gateway id, metadata, and forwarded
 * Anthropic-native input.
 */
export function stubAi(result: unknown) {
  const calls: AiStubCall[] = [];
  return {
    calls,
    async run(
      model: string,
      input: Record<string, unknown>,
      options?: AiStubCall["options"],
    ): Promise<unknown> {
      calls.push({ model, input, options });
      if (result instanceof Error) throw result;
      return result;
    },
  };
}

let saCounter = 0;

/** Generate an RSA PKCS#8 PEM so integrity tests can sign a real JWT.
 * Each call uses a unique client_email so the in-memory token cache in
 * integrity.ts does not bleed across tests. */
export async function generateServiceAccountJson(): Promise<string> {
  const email = `test-${saCounter++}@nimaz.iam.gserviceaccount.com`;
  const pair = (await crypto.subtle.generateKey(
    {
      name: "RSASSA-PKCS1-v1_5",
      modulusLength: 2048,
      publicExponent: new Uint8Array([1, 0, 1]),
      hash: "SHA-256",
    },
    true,
    ["sign", "verify"],
  )) as CryptoKeyPair;
  const pkcs8 = new Uint8Array(
    (await crypto.subtle.exportKey("pkcs8", pair.privateKey)) as ArrayBuffer,
  );
  let bin = "";
  for (const b of pkcs8) bin += String.fromCharCode(b);
  const b64 = btoa(bin).replace(/(.{64})/g, "$1\n");
  const pem = `-----BEGIN PRIVATE KEY-----\n${b64}\n-----END PRIVATE KEY-----\n`;
  return JSON.stringify({
    client_email: email,
    private_key: pem,
    token_uri: "https://oauth2.googleapis.com/token",
  });
}
