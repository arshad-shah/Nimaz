# Nimaz bug report — optional Cloud Function

This directory is **optional** and is **not** part of the Android app. It contains
a single Cloud Function that notifies a chat channel whenever a user submits a bug
report, so the team gets a push instead of having to poll the Firebase console.

The app is fully functional without this: reports are written to the `bug_reports`
Firestore collection and screenshots to Cloud Storage regardless, and can always
be read in the Firebase console.

## What it does

`onBugReportCreated` triggers on `onCreate` of `bug_reports/{reportId}`, formats
the report plus its diagnostics, and POSTs a summary to an incoming webhook
(Discord/Slack-compatible — both accept a JSON body with a `content` field).

## Setup

1. Install dependencies:
   ```bash
   cd functions
   npm install
   ```
2. Configure the webhook URL — **do not hardcode it** in source. Either set a
   function parameter/secret or an environment variable:
   ```bash
   firebase functions:secrets:set NOTIFIER_WEBHOOK_URL
   # or, for non-secret config:
   # export NOTIFIER_WEBHOOK_URL="https://your-webhook"
   ```
   The placeholder in `index.js` (`REPLACE_WITH_YOUR_WEBHOOK_URL`) is intentional;
   the function no-ops until a real URL is provided.
3. Deploy:
   ```bash
   firebase deploy --only functions
   ```

## Related backend config

The matching security rules live at the repository root:

- `firestore.rules` — authenticated `create`-only on `bug_reports`.
- `storage.rules` — authenticated `create`-only under `bug_reports/`.

Deploy them with:
```bash
firebase deploy --only firestore:rules
firebase deploy --only storage
```

## Recommended hardening

Enable **App Check** (Play Integrity) in the Firebase console so only genuine app
installs can write, and add the `firebase-appcheck-playintegrity` dependency to the
Android app to attach attestation tokens.
