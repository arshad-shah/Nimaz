import { env, fetchMock } from "cloudflare:test";
import { afterEach, beforeAll, describe, expect, it } from "vitest";
import app from "../src/index";
import { checkIntegrity } from "../src/middleware/integrity";
import {
  generateServiceAccountJson,
  makeAssistInput,
  makeEnvelope,
} from "./helpers";

// checkIntegrity never throws. It is the Worker's only guard: an explicit
// failed verdict returns "failed" (the dispatcher rejects with
// ATTESTATION_FAILED/403); every path where verification cannot run returns
// "unavailable" and the request proceeds (fail-open). Throttling/cost caps
// live in the AI Gateway, not here.
describe("integrity verification", () => {
  beforeAll(() => {
    fetchMock.activate();
    fetchMock.disableNetConnect();
  });
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
        "a-real-looking-integrity-token",
        Date.now(),
      ),
    ).resolves.toBe("unavailable");
  });

  it("returns unavailable when the token is missing or too short", async () => {
    const envNoSkip = {
      ...env,
      SKIP_ATTESTATION: "false",
      GOOGLE_SERVICE_ACCOUNT_JSON: await generateServiceAccountJson(),
    };
    await expect(checkIntegrity(envNoSkip, "", Date.now())).resolves.toBe(
      "unavailable",
    );
    await expect(checkIntegrity(envNoSkip, "short", Date.now())).resolves.toBe(
      "unavailable",
    );
  });

  it("verifies a token with PLAY_RECOGNIZED + MEETS_DEVICE_INTEGRITY", async () => {
    const sa = await generateServiceAccountJson();
    fetchMock
      .get("https://oauth2.googleapis.com")
      .intercept({ path: "/token", method: "POST" })
      .reply(200, { access_token: "ya29.test", expires_in: 3600 });
    fetchMock
      .get("https://playintegrity.googleapis.com")
      .intercept({
        path: "/v1/com.arshadshah.nimaz:decodeIntegrityToken",
        method: "POST",
      })
      .reply(200, {
        tokenPayloadExternal: {
          appIntegrity: { appRecognitionVerdict: "PLAY_RECOGNIZED" },
          deviceIntegrity: {
            deviceRecognitionVerdict: ["MEETS_DEVICE_INTEGRITY"],
          },
          requestDetails: { requestPackageName: "com.arshadshah.nimaz" },
        },
      });

    await expect(
      checkIntegrity(
        { ...env, SKIP_ATTESTATION: "false", GOOGLE_SERVICE_ACCOUNT_JSON: sa },
        "a-real-looking-integrity-token",
        Date.now(),
      ),
    ).resolves.toBe("verified");
  });

  it("accepts MEETS_BASIC_INTEGRITY as sufficient device integrity", async () => {
    const sa = await generateServiceAccountJson();
    fetchMock
      .get("https://oauth2.googleapis.com")
      .intercept({ path: "/token", method: "POST" })
      .reply(200, { access_token: "ya29.test", expires_in: 3600 });
    fetchMock
      .get("https://playintegrity.googleapis.com")
      .intercept({
        path: "/v1/com.arshadshah.nimaz:decodeIntegrityToken",
        method: "POST",
      })
      .reply(200, {
        tokenPayloadExternal: {
          appIntegrity: { appRecognitionVerdict: "PLAY_RECOGNIZED" },
          deviceIntegrity: {
            deviceRecognitionVerdict: ["MEETS_BASIC_INTEGRITY"],
          },
        },
      });

    await expect(
      checkIntegrity(
        { ...env, SKIP_ATTESTATION: "false", GOOGLE_SERVICE_ACCOUNT_JSON: sa },
        "a-real-looking-integrity-token",
        Date.now(),
      ),
    ).resolves.toBe("verified");
  });

  it("fails an unrecognized app verdict (never throws)", async () => {
    const sa = await generateServiceAccountJson();
    fetchMock
      .get("https://oauth2.googleapis.com")
      .intercept({ path: "/token", method: "POST" })
      .reply(200, { access_token: "ya29.test", expires_in: 3600 });
    fetchMock
      .get("https://playintegrity.googleapis.com")
      .intercept({
        path: "/v1/com.arshadshah.nimaz:decodeIntegrityToken",
        method: "POST",
      })
      .reply(200, {
        tokenPayloadExternal: {
          appIntegrity: { appRecognitionVerdict: "UNRECOGNIZED_VERSION" },
          deviceIntegrity: { deviceRecognitionVerdict: [] },
        },
      });

    await expect(
      checkIntegrity(
        { ...env, SKIP_ATTESTATION: "false", GOOGLE_SERVICE_ACCOUNT_JSON: sa },
        "a-real-looking-integrity-token",
        Date.now(),
      ),
    ).resolves.toBe("failed");
  });

  it("returns unavailable when the Play Integrity API is down", async () => {
    const sa = await generateServiceAccountJson();
    fetchMock
      .get("https://oauth2.googleapis.com")
      .intercept({ path: "/token", method: "POST" })
      .reply(200, { access_token: "ya29.test", expires_in: 3600 });
    fetchMock
      .get("https://playintegrity.googleapis.com")
      .intercept({
        path: "/v1/com.arshadshah.nimaz:decodeIntegrityToken",
        method: "POST",
      })
      .reply(503, { error: "unavailable" });

    await expect(
      checkIntegrity(
        { ...env, SKIP_ATTESTATION: "false", GOOGLE_SERVICE_ACCOUNT_JSON: sa },
        "a-real-looking-integrity-token",
        Date.now(),
      ),
    ).resolves.toBe("unavailable");
  });

  it("returns unavailable when Google token minting fails", async () => {
    const sa = await generateServiceAccountJson();
    fetchMock
      .get("https://oauth2.googleapis.com")
      .intercept({ path: "/token", method: "POST" })
      .reply(500, { error: "boom" });

    await expect(
      checkIntegrity(
        { ...env, SKIP_ATTESTATION: "false", GOOGLE_SERVICE_ACCOUNT_JSON: sa },
        "a-real-looking-integrity-token",
        Date.now(),
      ),
    ).resolves.toBe("unavailable");
  });
});

describe("integrity gating (integration)", () => {
  beforeAll(() => {
    fetchMock.activate();
    fetchMock.disableNetConnect();
  });
  afterEach(() => fetchMock.assertNoPendingInterceptors());

  it("rejects a failed verdict with ATTESTATION_FAILED/403", async () => {
    const sa = await generateServiceAccountJson();
    fetchMock
      .get("https://oauth2.googleapis.com")
      .intercept({ path: "/token", method: "POST" })
      .reply(200, { access_token: "ya29.test", expires_in: 3600 });
    fetchMock
      .get("https://playintegrity.googleapis.com")
      .intercept({
        path: "/v1/com.arshadshah.nimaz:decodeIntegrityToken",
        method: "POST",
      })
      .reply(200, {
        tokenPayloadExternal: {
          appIntegrity: { appRecognitionVerdict: "UNRECOGNIZED_VERSION" },
          deviceIntegrity: { deviceRecognitionVerdict: [] },
        },
      });

    const res = await app.request(
      "/v1/invoke",
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(makeEnvelope(makeAssistInput())),
      },
      { ...env, SKIP_ATTESTATION: "false", GOOGLE_SERVICE_ACCOUNT_JSON: sa },
    );
    expect(res.status).toBe(403);
    expect(((await res.json()) as any).error.code).toBe("ATTESTATION_FAILED");
  });
});
