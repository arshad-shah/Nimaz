import { env, fetchMock } from "cloudflare:test";
import { afterEach, beforeAll, describe, expect, it } from "vitest";
import { verifyIntegrity } from "../src/middleware/integrity";
import { generateServiceAccountJson } from "./helpers";

describe("integrity middleware", () => {
  beforeAll(() => {
    fetchMock.activate();
    fetchMock.disableNetConnect();
  });
  afterEach(() => fetchMock.assertNoPendingInterceptors());

  it("bypasses verification entirely when SKIP_ATTESTATION is true", async () => {
    // No fetch interceptors registered → any network call would throw.
    await expect(
      verifyIntegrity({ ...env, SKIP_ATTESTATION: "true" }, "anything", Date.now()),
    ).resolves.toBeUndefined();
  });

  it("accepts a token with PLAY_RECOGNIZED + MEETS_DEVICE_INTEGRITY", async () => {
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
      verifyIntegrity(
        { ...env, SKIP_ATTESTATION: "false", GOOGLE_SERVICE_ACCOUNT_JSON: sa },
        "a-real-looking-integrity-token",
        Date.now(),
      ),
    ).resolves.toBeUndefined();
  });

  it("rejects a token with an unrecognized app verdict", async () => {
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
      verifyIntegrity(
        { ...env, SKIP_ATTESTATION: "false", GOOGLE_SERVICE_ACCOUNT_JSON: sa },
        "a-real-looking-integrity-token",
        Date.now(),
      ),
    ).rejects.toMatchObject({ code: "ATTESTATION_FAILED" });
  });
});
