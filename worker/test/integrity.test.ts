import { env, fetchMock } from "cloudflare:test";
import { afterEach, beforeAll, describe, expect, it } from "vitest";
import { checkIntegrity } from "../src/middleware/integrity";
import { generateServiceAccountJson } from "./helpers";

// checkIntegrity never throws — it classifies each request into a trust tier
// ("verified" gets the full daily cap, "unverified" the small one). These tests
// pin every degradation path to "unverified" and the happy path to "verified".
describe("integrity classification", () => {
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

  it("returns unverified when no service account is configured", async () => {
    await expect(
      checkIntegrity(
        { ...env, SKIP_ATTESTATION: "false", GOOGLE_SERVICE_ACCOUNT_JSON: undefined },
        "a-real-looking-integrity-token",
        Date.now(),
      ),
    ).resolves.toBe("unverified");
  });

  it("returns unverified when the token is missing or too short", async () => {
    const envNoSkip = {
      ...env,
      SKIP_ATTESTATION: "false",
      GOOGLE_SERVICE_ACCOUNT_JSON: await generateServiceAccountJson(),
    };
    await expect(checkIntegrity(envNoSkip, "", Date.now())).resolves.toBe(
      "unverified",
    );
    await expect(checkIntegrity(envNoSkip, "short", Date.now())).resolves.toBe(
      "unverified",
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

  it("demotes an unrecognized app verdict to unverified (never throws)", async () => {
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
    ).resolves.toBe("unverified");
  });

  it("demotes to unverified when the Play Integrity API is down", async () => {
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
    ).resolves.toBe("unverified");
  });

  it("demotes to unverified when Google token minting fails", async () => {
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
    ).resolves.toBe("unverified");
  });
});
