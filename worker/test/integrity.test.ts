import { env, fetchMock } from "cloudflare:test";
import { afterEach, beforeAll, beforeEach, describe, expect, it } from "vitest";
import app from "../src/index";
import {
  checkIntegrity,
  MAX_CONSECUTIVE_FAIL_OPEN,
  resetIntegrityOutageState,
} from "../src/middleware/integrity";
import {
  generateServiceAccountJson,
  makeAssistInput,
  makeEnvelope,
} from "./helpers";

const DECODE_HOST = "https://playintegrity.googleapis.com";
const DECODE_PATH = "/v1/com.arshadshah.nimaz:decodeIntegrityToken";
const REAL_TOKEN = "a-real-looking-integrity-token";

/** Mint interceptor — one per service account (the access token is cached). */
function mockTokenMint(status = 200, body: object = {
  access_token: "ya29.test",
  expires_in: 3600,
}) {
  fetchMock
    .get("https://oauth2.googleapis.com")
    .intercept({ path: "/token", method: "POST" })
    .reply(status, body);
}

function mockDecode(status: number, body: object, times = 1) {
  fetchMock
    .get(DECODE_HOST)
    .intercept({ path: DECODE_PATH, method: "POST" })
    .reply(status, body)
    .times(times);
}

/** A decoded payload that passes every check, with the given overrides. */
function verdict(overrides: Record<string, unknown> = {}) {
  return {
    tokenPayloadExternal: {
      appIntegrity: { appRecognitionVerdict: "PLAY_RECOGNIZED" },
      deviceIntegrity: { deviceRecognitionVerdict: ["MEETS_DEVICE_INTEGRITY"] },
      requestDetails: { requestPackageName: "com.arshadshah.nimaz" },
      ...overrides,
    },
  };
}

async function liveEnv() {
  return {
    ...env,
    SKIP_ATTESTATION: "false",
    GOOGLE_SERVICE_ACCOUNT_JSON: await generateServiceAccountJson(),
  };
}

// checkIntegrity never throws. It is the Worker's only guard. Anything the
// caller controls — no token, a short token, a token Google won't decode, a
// verdict that doesn't clear app/device/package — is "failed" and the
// dispatcher rejects it (ATTESTATION_FAILED/403). Only reasons outside the
// caller's control (no service account, Google-side error) fail open, and only
// within the bounded window. Throttling/cost caps live in the AI Gateway.
describe("integrity verification", () => {
  beforeAll(() => {
    fetchMock.activate();
    fetchMock.disableNetConnect();
  });
  beforeEach(() => resetIntegrityOutageState());
  afterEach(() => fetchMock.assertNoPendingInterceptors());

  it("returns verified when SKIP_ATTESTATION is true (no network)", async () => {
    // No fetch interceptors registered → any network call would throw.
    await expect(
      checkIntegrity({ ...env, SKIP_ATTESTATION: "true" }, "anything", Date.now()),
    ).resolves.toBe("verified");
  });

  it("returns unavailable when no service account is configured", async () => {
    await expect(
      checkIntegrity(
        { ...env, SKIP_ATTESTATION: "false", GOOGLE_SERVICE_ACCOUNT_JSON: undefined },
        REAL_TOKEN,
        Date.now(),
      ),
    ).resolves.toBe("unavailable");
  });

  it("fails an empty token without calling Google", async () => {
    // No interceptors: reaching the network here would throw, so this also
    // proves the empty token short-circuits before any Google call.
    await expect(checkIntegrity(await liveEnv(), "", Date.now())).resolves.toBe(
      "failed",
    );
  });

  it("fails a token too short to be real, without calling Google", async () => {
    const live = await liveEnv();
    await expect(checkIntegrity(live, "short", Date.now())).resolves.toBe(
      "failed",
    );
    await expect(checkIntegrity(live, "123456789", Date.now())).resolves.toBe(
      "failed",
    );
  });

  it("verifies a token with PLAY_RECOGNIZED + MEETS_DEVICE_INTEGRITY", async () => {
    mockTokenMint();
    mockDecode(200, verdict());

    await expect(
      checkIntegrity(await liveEnv(), REAL_TOKEN, Date.now()),
    ).resolves.toBe("verified");
  });

  it("accepts MEETS_BASIC_INTEGRITY as sufficient device integrity", async () => {
    mockTokenMint();
    mockDecode(
      200,
      verdict({
        deviceIntegrity: { deviceRecognitionVerdict: ["MEETS_BASIC_INTEGRITY"] },
      }),
    );

    await expect(
      checkIntegrity(await liveEnv(), REAL_TOKEN, Date.now()),
    ).resolves.toBe("verified");
  });

  it("fails an unrecognized app verdict (never throws)", async () => {
    mockTokenMint();
    mockDecode(
      200,
      verdict({
        appIntegrity: { appRecognitionVerdict: "UNRECOGNIZED_VERSION" },
        deviceIntegrity: { deviceRecognitionVerdict: [] },
      }),
    );

    await expect(
      checkIntegrity(await liveEnv(), REAL_TOKEN, Date.now()),
    ).resolves.toBe("failed");
  });

  it("fails a verdict with no requestDetails at all", async () => {
    mockTokenMint();
    mockDecode(200, {
      tokenPayloadExternal: {
        appIntegrity: { appRecognitionVerdict: "PLAY_RECOGNIZED" },
        deviceIntegrity: {
          deviceRecognitionVerdict: ["MEETS_DEVICE_INTEGRITY"],
        },
      },
    });

    await expect(
      checkIntegrity(await liveEnv(), REAL_TOKEN, Date.now()),
    ).resolves.toBe("failed");
  });

  it("fails a verdict whose requestDetails names another package", async () => {
    mockTokenMint();
    mockDecode(
      200,
      verdict({ requestDetails: { requestPackageName: "com.someone.else" } }),
    );

    await expect(
      checkIntegrity(await liveEnv(), REAL_TOKEN, Date.now()),
    ).resolves.toBe("failed");
  });

  it("fails a 200 response with no decoded payload", async () => {
    mockTokenMint();
    mockDecode(200, {});

    await expect(
      checkIntegrity(await liveEnv(), REAL_TOKEN, Date.now()),
    ).resolves.toBe("failed");
  });

  it("fails a token Google refuses to decode (400)", async () => {
    mockTokenMint();
    mockDecode(400, { error: { status: "INVALID_ARGUMENT" } });

    await expect(
      checkIntegrity(
        await liveEnv(),
        "not-a-real-token-just-long-enough",
        Date.now(),
      ),
    ).resolves.toBe("failed");
  });

  it("returns unavailable when the Play Integrity API is down", async () => {
    mockTokenMint();
    mockDecode(503, { error: "unavailable" });

    await expect(
      checkIntegrity(await liveEnv(), REAL_TOKEN, Date.now()),
    ).resolves.toBe("unavailable");
  });

  it("returns unavailable when Google token minting fails", async () => {
    mockTokenMint(500, { error: "boom" });

    await expect(
      checkIntegrity(await liveEnv(), REAL_TOKEN, Date.now()),
    ).resolves.toBe("unavailable");
  });

  it("stops failing open past the bounded outage window", async () => {
    const live = await liveEnv();
    const attempts = MAX_CONSECUTIVE_FAIL_OPEN + 1;
    mockTokenMint();
    mockDecode(503, { error: "unavailable" }, attempts);

    const results: string[] = [];
    for (let i = 0; i < attempts; i++) {
      results.push(await checkIntegrity(live, REAL_TOKEN, Date.now()));
    }

    expect(results.slice(0, MAX_CONSECUTIVE_FAIL_OPEN)).toEqual(
      Array(MAX_CONSECUTIVE_FAIL_OPEN).fill("unavailable"),
    );
    // Past the bound an induced outage is no longer a way through.
    expect(results[MAX_CONSECUTIVE_FAIL_OPEN]).toBe("failed");
  });

  it("resets the fail-open budget once Google answers a decode again", async () => {
    const live = await liveEnv();
    const now = Date.now();
    mockTokenMint();
    mockDecode(503, { error: "unavailable" }, MAX_CONSECUTIVE_FAIL_OPEN);
    for (let i = 0; i < MAX_CONSECUTIVE_FAIL_OPEN; i++) {
      await checkIntegrity(live, REAL_TOKEN, now);
    }

    // Google recovers…
    mockDecode(200, verdict());
    await expect(checkIntegrity(live, REAL_TOKEN, now)).resolves.toBe(
      "verified",
    );

    // …so the next blip is fail-open again rather than instantly closed.
    mockDecode(503, { error: "unavailable" });
    await expect(checkIntegrity(live, REAL_TOKEN, now)).resolves.toBe(
      "unavailable",
    );
  });

  it("starts a fresh fail-open run after the outage window lapses", async () => {
    const live = await liveEnv();
    const start = Date.now();
    mockTokenMint();
    mockDecode(503, { error: "unavailable" }, MAX_CONSECUTIVE_FAIL_OPEN + 1);
    for (let i = 0; i < MAX_CONSECUTIVE_FAIL_OPEN; i++) {
      await checkIntegrity(live, REAL_TOKEN, start);
    }

    // Ten minutes later the earlier run is stale (the window is five) — a lone
    // blip must not be punished for failures that happened long ago.
    await expect(
      checkIntegrity(live, REAL_TOKEN, start + 10 * 60_000),
    ).resolves.toBe("unavailable");
  });
});

describe("integrity gating (integration)", () => {
  beforeAll(() => {
    fetchMock.activate();
    fetchMock.disableNetConnect();
  });
  beforeEach(() => resetIntegrityOutageState());
  afterEach(() => fetchMock.assertNoPendingInterceptors());

  it("rejects a failed verdict with ATTESTATION_FAILED/403", async () => {
    mockTokenMint();
    mockDecode(
      200,
      verdict({
        appIntegrity: { appRecognitionVerdict: "UNRECOGNIZED_VERSION" },
        deviceIntegrity: { deviceRecognitionVerdict: [] },
      }),
    );

    const res = await app.request(
      "/v1/invoke",
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(makeEnvelope(makeAssistInput())),
      },
      await liveEnv(),
    );
    expect(res.status).toBe(403);
    expect(((await res.json()) as any).error.code).toBe("ATTESTATION_FAILED");
  });

  it("rejects an empty integrityToken with ATTESTATION_FAILED/403", async () => {
    // The reported bypass: no token, no Google call, a real Claude answer.
    // No interceptors are registered, so a Claude call would throw too.
    const res = await app.request(
      "/v1/invoke",
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(
          makeEnvelope(makeAssistInput(), { integrityToken: "" }),
        ),
      },
      await liveEnv(),
    );
    expect(res.status).toBe(403);
    expect(((await res.json()) as any).error.code).toBe("ATTESTATION_FAILED");
  });

  it("rejects a short integrityToken with ATTESTATION_FAILED/403", async () => {
    const res = await app.request(
      "/v1/invoke",
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(
          makeEnvelope(makeAssistInput(), { integrityToken: "abc" }),
        ),
      },
      await liveEnv(),
    );
    expect(res.status).toBe(403);
    expect(((await res.json()) as any).error.code).toBe("ATTESTATION_FAILED");
  });
});
