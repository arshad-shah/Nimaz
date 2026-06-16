/**
 * OPTIONAL Cloud Function for the Nimaz bug report system.
 *
 * This is NOT part of the Android app. It is an independent, optional backend
 * that fires when a new document is created in the `bug_reports` collection and
 * posts a formatted summary to a notification channel (here: a Discord/Slack-style
 * incoming webhook). Deploy it only if you want push notifications for new reports;
 * the app works without it (reports are still readable in the Firebase console).
 *
 * Setup:
 *   1. cd functions && npm install
 *   2. Set the webhook URL (do NOT hardcode it):
 *        firebase functions:config:set notifier.webhook_url="https://..."
 *      or use an environment variable / Secret Manager for 2nd-gen functions.
 *   3. firebase deploy --only functions
 *
 * The webhook URL below is a documented placeholder and must be replaced.
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { defineString } = require("firebase-functions/params");

// Replace via config/secret — leave as a placeholder in source.
const WEBHOOK_URL = defineString("NOTIFIER_WEBHOOK_URL", {
  default: "https://example.com/REPLACE_WITH_YOUR_WEBHOOK_URL",
});

exports.onBugReportCreated = onDocumentCreated("bug_reports/{reportId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;

  const report = snapshot.data();
  const reportId = event.params.reportId;
  const diagnostics = report.diagnostics || {};

  const lines = [
    `New Nimaz bug report: ${reportId}`,
    `Category: ${report.category || "unknown"}`,
    `Description: ${report.description || ""}`,
  ];
  if (report.stepsToReproduce) lines.push(`Steps: ${report.stepsToReproduce}`);
  if (report.contactEmail) lines.push(`Contact: ${report.contactEmail}`);
  if (report.screenshotPath) lines.push(`Screenshot: ${report.screenshotPath}`);
  if (diagnostics.appVersionName) {
    lines.push(
      `App ${diagnostics.appVersionName} (${diagnostics.appVersionCode}) | ` +
        `${diagnostics.deviceManufacturer} ${diagnostics.deviceModel} | ` +
        `Android ${diagnostics.androidVersion} (API ${diagnostics.apiLevel})`
    );
    lines.push(
      `Calc: ${diagnostics.calculationMethod} | Asr: ${diagnostics.asrMethod} | ` +
        `HighLat: ${diagnostics.highLatitudeRule} | Location: ${diagnostics.locationMode}`
    );
    lines.push(
      `Notif: ${diagnostics.notificationsPermissionGranted} | ` +
        `ExactAlarm: ${diagnostics.exactAlarmPermissionGranted} | ` +
        `BatteryExempt: ${diagnostics.batteryOptimizationExempt}`
    );
  }

  const content = lines.join("\n");
  const url = WEBHOOK_URL.value();

  if (!url || url.includes("REPLACE_WITH_YOUR_WEBHOOK_URL")) {
    console.warn("Notifier webhook URL not configured; skipping notification.");
    return;
  }

  try {
    // Node 18+ runtime provides a global fetch.
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      // "content" is the field most chat webhooks (Discord/Slack-compatible) expect.
      body: JSON.stringify({ content }),
    });
    if (!response.ok) {
      console.error(`Notifier webhook failed: ${response.status} ${response.statusText}`);
    }
  } catch (err) {
    console.error("Notifier webhook error", err);
  }
});
