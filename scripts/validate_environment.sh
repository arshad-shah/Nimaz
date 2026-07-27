#!/usr/bin/env bash
#
# Nimaz environment validator.
#
# Checks that this machine can build, test and work on the repo: JDK, Android
# SDK (platform / build-tools / licenses), the Gradle wrapper, the Ruby+Fastlane
# lane used by CI, the Node toolchain for the Cloudflare Worker, Python for the
# tajweed checks, and the shipped database asset.
#
# The expected versions are read out of the repo itself (app/build.gradle.kts,
# gradle/wrapper/gradle-wrapper.properties, Gemfile, worker/package.json) so
# this script cannot drift from the build files.
#
#     ./scripts/validate_environment.sh            # fast checks only
#     ./scripts/validate_environment.sh --full     # also compiles Kotlin (slow)
#     ./scripts/validate_environment.sh --quiet     # only WARN/FAIL lines
#
# Exit codes: 0 = usable (may include warnings), 1 = at least one hard failure.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT" || exit 1

FULL=0
QUIET=0
for arg in "$@"; do
  case "$arg" in
    --full) FULL=1 ;;
    --quiet) QUIET=1 ;;
    -h|--help)
      sed -n '3,20p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      echo "unknown option: $arg (try --help)" >&2
      exit 2
      ;;
  esac
done

if [ -t 1 ]; then
  C_RESET=$'\033[0m'; C_GREEN=$'\033[32m'; C_YELLOW=$'\033[33m'
  C_RED=$'\033[31m';  C_BOLD=$'\033[1m';   C_DIM=$'\033[2m'
else
  C_RESET=""; C_GREEN=""; C_YELLOW=""; C_RED=""; C_BOLD=""; C_DIM=""
fi

PASS_COUNT=0
WARN_COUNT=0
FAIL_COUNT=0
# Failures in the toolchain itself (JDK/SDK/Gradle) — these make a compile
# pointless, so --full skips the build when any are present. Content failures
# (a missing asset) are still failures but do not block compilation.
TOOLCHAIN_FAIL_COUNT=0
FAILURES=()
WARNINGS=()

section() { [ "$QUIET" -eq 1 ] && return 0; printf '\n%s%s%s\n' "$C_BOLD" "$1" "$C_RESET"; }
pass() { PASS_COUNT=$((PASS_COUNT + 1)); [ "$QUIET" -eq 1 ] && return 0; printf '  %s✔%s %s\n' "$C_GREEN" "$C_RESET" "$1"; }
warn() { WARN_COUNT=$((WARN_COUNT + 1)); WARNINGS+=("$1"); printf '  %s!%s %s\n' "$C_YELLOW" "$C_RESET" "$1"; }
# fail <message> [toolchain]  — pass "toolchain" for anything that breaks the build itself
fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  FAILURES+=("$1")
  [ "${2:-}" = "toolchain" ] && TOOLCHAIN_FAIL_COUNT=$((TOOLCHAIN_FAIL_COUNT + 1))
  printf '  %s✘%s %s\n' "$C_RED" "$C_RESET" "$1"
}
hint() { [ "$QUIET" -eq 1 ] && return 0; printf '    %s%s%s\n' "$C_DIM" "$1" "$C_RESET"; }

have() { command -v "$1" >/dev/null 2>&1; }

# ── expectations read from the repo ──────────────────────────────────────────

APP_GRADLE="app/build.gradle.kts"
COMPILE_SDK="$(sed -n 's/^[[:space:]]*compileSdk[[:space:]]*=[[:space:]]*\([0-9]\{1,\}\).*/\1/p' "$APP_GRADLE" | head -1)"
MIN_SDK="$(sed -n 's/^[[:space:]]*minSdk[[:space:]]*=[[:space:]]*\([0-9]\{1,\}\).*/\1/p' "$APP_GRADLE" | head -1)"
TARGET_SDK="$(sed -n 's/^[[:space:]]*targetSdk[[:space:]]*=[[:space:]]*\([0-9]\{1,\}\).*/\1/p' "$APP_GRADLE" | head -1)"
REQUIRED_JDK="$(sed -n 's/.*JavaVersion\.VERSION_\([0-9]\{1,\}\).*/\1/p' "$APP_GRADLE" | head -1)"
GRADLE_VERSION="$(sed -n 's/^distributionUrl=.*gradle-\([0-9.]\{1,\}\)-.*\.zip/\1/p' gradle/wrapper/gradle-wrapper.properties | head -1)"

: "${COMPILE_SDK:=36}"
: "${REQUIRED_JDK:=21}"

# ── repo sanity ──────────────────────────────────────────────────────────────

section "Repository"

if [ -f settings.gradle.kts ] && [ -f "$APP_GRADLE" ]; then
  pass "Nimaz repo root: $REPO_ROOT"
else
  fail "not a Nimaz checkout (missing settings.gradle.kts or $APP_GRADLE)"
  echo
  printf '%s1 failure.%s Run this from inside the repository.\n' "$C_RED" "$C_RESET"
  exit 1
fi

if git rev-parse --git-dir >/dev/null 2>&1; then
  branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null)"
  pass "git repository (branch: ${branch:-detached})"
  if [ "$branch" = "dev" ] || [ "$branch" = "main" ] || [ "$branch" = "master" ]; then
    warn "on '$branch' — develop on a feature branch (CLAUDE.md)"
  fi
else
  warn "not a git checkout — release/version tooling expects git history"
fi

avail_kb="$(df -Pk . 2>/dev/null | awk 'NR==2 {print $4}')"
if [ -n "${avail_kb:-}" ]; then
  avail_gb=$((avail_kb / 1024 / 1024))
  if [ "$avail_kb" -lt 3145728 ]; then
    warn "only ${avail_gb}GB free disk — an Android build needs ~5GB for caches"
  else
    pass "disk space: ${avail_gb}GB available"
  fi
fi

# ── JDK ──────────────────────────────────────────────────────────────────────

section "JDK (required: $REQUIRED_JDK)"

if have java; then
  java_raw="$(java -version 2>&1 | grep -Ei 'version "' | head -1)"
  java_ver="$(printf '%s' "$java_raw" | sed -n 's/.*version "\([0-9]\{1,\}\)\(\.[0-9]*\)*.*/\1/p')"
  if [ -z "$java_ver" ]; then
    warn "could not parse java version from: $java_raw"
  elif [ "$java_ver" -eq "$REQUIRED_JDK" ]; then
    pass "java $(printf '%s' "$java_raw" | sed 's/.*version "\([^"]*\)".*/\1/')"
  elif [ "$java_ver" -gt "$REQUIRED_JDK" ]; then
    warn "java $java_ver is newer than the required JDK $REQUIRED_JDK — AGP may reject it"
    hint "install JDK $REQUIRED_JDK and point JAVA_HOME at it"
  else
    fail "java $java_ver is too old — JDK $REQUIRED_JDK required" toolchain
    hint "install a JDK $REQUIRED_JDK (e.g. Temurin) and set JAVA_HOME"
  fi
else
  fail "java not found on PATH" toolchain
  hint "install JDK $REQUIRED_JDK and set JAVA_HOME"
fi

if [ -n "${JAVA_HOME:-}" ]; then
  if [ -x "$JAVA_HOME/bin/java" ]; then
    pass "JAVA_HOME=$JAVA_HOME"
  else
    fail "JAVA_HOME=$JAVA_HOME has no bin/java" toolchain
  fi
else
  warn "JAVA_HOME is not set — Gradle falls back to the java on PATH"
fi

# ── Android SDK ──────────────────────────────────────────────────────────────

section "Android SDK (compileSdk $COMPILE_SDK, minSdk ${MIN_SDK:-?}, targetSdk ${TARGET_SDK:-?})"

SDK_DIR=""
SDK_SOURCE=""
if [ -f local.properties ]; then
  sdk_from_props="$(sed -n 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*\(.*\)$/\1/p' local.properties | head -1 | tr -d '\r')"
  # local.properties escapes ':' and '\' Java-properties style
  sdk_from_props="${sdk_from_props//\\:/:}"
  sdk_from_props="${sdk_from_props//\\\\//}"
  if [ -n "$sdk_from_props" ]; then
    SDK_DIR="$sdk_from_props"
    SDK_SOURCE="local.properties"
  fi
fi
if [ -z "$SDK_DIR" ] && [ -n "${ANDROID_HOME:-}" ]; then
  SDK_DIR="$ANDROID_HOME"; SDK_SOURCE="ANDROID_HOME"
fi
if [ -z "$SDK_DIR" ] && [ -n "${ANDROID_SDK_ROOT:-}" ]; then
  SDK_DIR="$ANDROID_SDK_ROOT"; SDK_SOURCE="ANDROID_SDK_ROOT"
fi

if [ -z "$SDK_DIR" ]; then
  fail "no Android SDK location (set sdk.dir in local.properties, or \$ANDROID_HOME)" toolchain
  hint "echo \"sdk.dir=\$HOME/Android/Sdk\" > local.properties"
elif [ ! -d "$SDK_DIR" ]; then
  fail "SDK path from $SDK_SOURCE does not exist: $SDK_DIR" toolchain
else
  pass "SDK at $SDK_DIR (from $SDK_SOURCE)"

  # platform for compileSdk — accept android-37 and versioned android-37.0
  if compgen -G "$SDK_DIR/platforms/android-${COMPILE_SDK}" >/dev/null 2>&1 ||
     compgen -G "$SDK_DIR/platforms/android-${COMPILE_SDK}.*" >/dev/null 2>&1; then
    found_platform="$(basename "$(ls -d "$SDK_DIR"/platforms/android-"${COMPILE_SDK}" "$SDK_DIR"/platforms/android-"${COMPILE_SDK}".* 2>/dev/null | head -1)")"
    pass "platform $found_platform installed"
  else
    installed="$(ls "$SDK_DIR/platforms" 2>/dev/null | tr '\n' ' ')"
    fail "platform android-$COMPILE_SDK missing (installed: ${installed:-none})" toolchain
    hint "sdkmanager \"platforms;android-$COMPILE_SDK\""
  fi

  # build-tools — need one at least as new as compileSdk
  best_bt=""
  if [ -d "$SDK_DIR/build-tools" ]; then
    best_bt="$(ls "$SDK_DIR/build-tools" 2>/dev/null | sort -V | tail -1)"
  fi
  if [ -z "$best_bt" ]; then
    fail "no build-tools installed" toolchain
    hint "sdkmanager \"build-tools;${COMPILE_SDK}.0.0\""
  else
    bt_major="${best_bt%%.*}"
    if [ "$bt_major" -ge "$COMPILE_SDK" ] 2>/dev/null; then
      pass "build-tools $best_bt"
    else
      warn "newest build-tools is $best_bt, older than compileSdk $COMPILE_SDK"
      hint "sdkmanager \"build-tools;${COMPILE_SDK}.0.0\""
    fi
  fi

  if [ -d "$SDK_DIR/platform-tools" ]; then
    pass "platform-tools installed"
  else
    warn "platform-tools missing — adb/device installs unavailable"
    hint "sdkmanager \"platform-tools\""
  fi

  if [ -f "$SDK_DIR/licenses/android-sdk-license" ]; then
    pass "SDK licenses accepted"
  else
    fail "SDK licenses not accepted — Gradle will refuse to resolve SDK components" toolchain
    hint "yes | sdkmanager --licenses"
  fi

  if [ -d "$SDK_DIR/cmdline-tools" ]; then
    pass "cmdline-tools installed"
  else
    warn "cmdline-tools missing — sdkmanager unavailable for installing components"
  fi
fi

# ── Gradle ───────────────────────────────────────────────────────────────────

section "Gradle (wrapper: ${GRADLE_VERSION:-unknown})"

if [ -x ./gradlew ]; then
  pass "./gradlew is executable"
elif [ -f ./gradlew ]; then
  fail "./gradlew is not executable" toolchain
  hint "chmod +x gradlew"
else
  fail "./gradlew missing" toolchain
fi

if [ -f gradle/wrapper/gradle-wrapper.jar ]; then
  pass "gradle-wrapper.jar present"
else
  fail "gradle/wrapper/gradle-wrapper.jar missing — wrapper cannot bootstrap" toolchain
fi

if [ -n "$GRADLE_VERSION" ] && [ -d "${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists" ]; then
  if compgen -G "${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${GRADLE_VERSION}-*" >/dev/null 2>&1; then
    pass "Gradle $GRADLE_VERSION distribution already downloaded"
  else
    warn "Gradle $GRADLE_VERSION not in the wrapper cache — first build will download it"
  fi
fi

jvmargs="$(sed -n 's/^org\.gradle\.jvmargs=\(.*\)$/\1/p' gradle.properties | head -1)"
[ -n "$jvmargs" ] && pass "org.gradle.jvmargs=$jvmargs"

# ── Ruby / Fastlane (the CI lane) ────────────────────────────────────────────

section "Ruby + Fastlane (CI lane: bundle exec fastlane android test)"

if have ruby; then
  pass "ruby $(ruby --version 2>&1 | awk '{print $2}')"
else
  warn "ruby not found — fastlane lanes unavailable (Gradle builds still work)"
fi

if have bundle; then
  pass "bundler $(bundle --version 2>&1 | awk '{print $NF}')"
  if bundle check >/dev/null 2>&1; then
    pass "Gemfile dependencies installed"
  else
    warn "Gemfile dependencies not installed"
    hint "bundle install"
  fi
else
  warn "bundler not found — cannot run the fastlane CI lane"
  hint "gem install bundler && bundle install"
fi

# ── Node / Cloudflare Worker ─────────────────────────────────────────────────

section "Node (worker/ — Ask with Proof backend)"

if [ -d worker ]; then
  if have node; then
    node_ver="$(node --version 2>&1)"
    node_major="$(printf '%s' "$node_ver" | sed 's/^v\([0-9]*\).*/\1/')"
    if [ "${node_major:-0}" -ge 20 ] 2>/dev/null; then
      pass "node $node_ver"
    else
      warn "node $node_ver — wrangler/vitest expect Node 20+"
    fi
  else
    warn "node not found — worker/ cannot be built or tested"
  fi

  if have npm; then
    pass "npm $(npm --version 2>&1)"
  else
    warn "npm not found"
  fi

  if [ -d worker/node_modules ]; then
    pass "worker/node_modules installed"
  else
    warn "worker/node_modules missing"
    hint "npm --prefix worker ci"
  fi
else
  hint "worker/ not present — skipped"
fi

# ── Python (tajweed contrast check, data scripts) ────────────────────────────

section "Python (scripts/, nimaz-pro-data/)"

if have python3; then
  py_ver="$(python3 --version 2>&1 | awk '{print $2}')"
  py_major="${py_ver%%.*}"
  py_minor="$(printf '%s' "$py_ver" | cut -d. -f2)"
  if [ "${py_major:-0}" -ge 3 ] && [ "${py_minor:-0}" -ge 8 ] 2>/dev/null; then
    pass "python $py_ver"
  else
    warn "python $py_ver — scripts/ expects Python 3.8+"
  fi
  if [ -f scripts/check_tajweed_contrast.py ]; then
    if python3 -c "import ast,sys; ast.parse(open('scripts/check_tajweed_contrast.py').read())" >/dev/null 2>&1; then
      pass "scripts/check_tajweed_contrast.py parses"
    else
      fail "scripts/check_tajweed_contrast.py does not parse under this Python"
    fi
  fi
else
  warn "python3 not found — tajweed contrast + data scripts unavailable"
fi

# ── Shipped data assets ──────────────────────────────────────────────────────

section "Shipped assets"

# Large binaries (the prepopulated DB) are tracked with Git LFS.
if [ -f .gitattributes ] && grep -q 'filter=lfs' .gitattributes 2>/dev/null; then
  if git lfs version >/dev/null 2>&1; then
    pass "git-lfs installed ($(git lfs version 2>&1 | awk '{print $1}'))"
  else
    fail "git-lfs not installed, but .gitattributes tracks files with it"
    hint "install git-lfs, then: git lfs install && git lfs pull"
  fi
fi

DB_ASSET="app/src/main/assets/database/nimaz_prepopulated.db"
if [ -f "$DB_ASSET" ]; then
  if [ "$(head -c 8 "$DB_ASSET" 2>/dev/null)" = "version " ] &&
     grep -q 'git-lfs.github.com/spec' "$DB_ASSET" 2>/dev/null; then
    real_size="$(sed -n 's/^size \([0-9]\{1,\}\)/\1/p' "$DB_ASSET" | head -1)"
    fail "$DB_ASSET is an unfetched Git LFS pointer, not the real database"
    hint "the checkout has a $(wc -c <"$DB_ASSET" | tr -d ' ')-byte stub instead of ~$(( ${real_size:-0} / 1024 / 1024 ))MB of content"
    hint "the app compiles but crashes on first launch when Room opens the asset"
    hint "git lfs install && git lfs pull"
  else
    db_size="$(du -h "$DB_ASSET" 2>/dev/null | awk '{print $1}')"
    pass "prepopulated database present (${db_size:-?})"
  fi
else
  fail "$DB_ASSET missing — the app cannot seed its content database"
fi

# ── Optional: real compile ───────────────────────────────────────────────────

if [ "$FULL" -eq 1 ]; then
  section "Full build check (./gradlew :app:compileDebugKotlin)"
  if [ "$TOOLCHAIN_FAIL_COUNT" -gt 0 ]; then
    warn "skipping compile — fix the toolchain failures above first"
  else
    log="$(mktemp)"
    printf '  %s…%s running, this takes a few minutes\n' "$C_DIM" "$C_RESET"
    if ./gradlew --console=plain :app:compileDebugKotlin >"$log" 2>&1; then
      pass ":app:compileDebugKotlin succeeded (KSP validated Hilt + Room wiring)"
    else
      fail ":app:compileDebugKotlin failed"
      tail -25 "$log" | sed 's/^/    /'
      hint "full log: $log"
    fi
  fi
fi

# ── Summary ──────────────────────────────────────────────────────────────────

printf '\n%s%s%s\n' "$C_BOLD" "Summary" "$C_RESET"
printf '  %s%d passed%s  %s%d warning(s)%s  %s%d failure(s)%s\n' \
  "$C_GREEN" "$PASS_COUNT" "$C_RESET" \
  "$C_YELLOW" "$WARN_COUNT" "$C_RESET" \
  "$C_RED" "$FAIL_COUNT" "$C_RESET"

if [ "$FAIL_COUNT" -gt 0 ]; then
  printf '\n%sBlocking:%s\n' "$C_RED" "$C_RESET"
  for f in "${FAILURES[@]}"; do printf '  • %s\n' "$f"; done
  printf '\n%sEnvironment is NOT ready.%s\n' "$C_RED" "$C_RESET"
  exit 1
fi

if [ "$WARN_COUNT" -gt 0 ]; then
  printf '\n%sEnvironment can build the app; %d optional item(s) need attention.%s\n' \
    "$C_YELLOW" "$WARN_COUNT" "$C_RESET"
else
  printf '\n%sEnvironment is ready.%s\n' "$C_GREEN" "$C_RESET"
fi
exit 0
