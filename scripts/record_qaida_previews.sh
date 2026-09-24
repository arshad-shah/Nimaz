#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p feature/content/build
qaida_capture_dir="$(mktemp -d "$PWD/feature/content/build/qaida-record-XXXXXX")"
qaida_source_sha="$(git rev-parse HEAD)"
QAIDA_PREVIEW_DIR="$qaida_capture_dir" ./gradlew :feature:content:testDebugUnitTest --tests '*QaidaVisualCheckTest' --rerun-tasks
python3 scripts/publish_qaida_previews.py "$qaida_capture_dir" --source-sha "$qaida_source_sha"
