import type { AskInput } from "../src/schemas/ask";

export function makeAskInput(overrides: Partial<AskInput> = {}): AskInput {
  return {
    question: "What does the Quran say about patience?",
    passages: [
      {
        id: "quran:2:153",
        source: "quran",
        text: "O you who believe, seek help through patience and prayer. Indeed, Allah is with the patient.",
        meta: "Surah Al-Baqarah 153 (Sahih Intl)",
      },
    ],
    ...overrides,
  };
}

export function makeEnvelope(input: AskInput, extra: Record<string, unknown> = {}) {
  return {
    capability: "ask-with-proof",
    integrityToken: "debug-skip",
    deviceId: "test-device-0001",
    input,
    ...extra,
  };
}

/** A well-formed Anthropic Messages response that calls submit_answer. */
export function mockAnthropicToolResponse(
  toolInput: unknown,
  usage: { input_tokens: number; output_tokens: number; cache_read_input_tokens?: number } = {
    input_tokens: 500,
    output_tokens: 80,
  },
) {
  return {
    id: "msg_test",
    content: [
      {
        type: "tool_use",
        id: "toolu_test",
        name: "submit_answer",
        input: toolInput,
      },
    ],
    stop_reason: "tool_use",
    usage,
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
