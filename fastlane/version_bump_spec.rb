# Regression spec for the version bump in Fastfile.
#
# A deploy bumps the version, uploads to Play, and only then pushes the bump back
# to dev. Anything that derives the next version from the checked-out tree alone
# therefore picks the same version twice when two deploys overlap, and the second
# upload fails with "Version code N has already been used" — after a full release
# build has been paid for. That happened on 2026-07-30 (versionCode 380).
#
# The bump now takes its baseline from what has actually been published: the
# version codes Play reports, and the release tags. These scenarios pin that
# behaviour. Run with: ruby fastlane/version_bump_spec.rb
#
# The spec drives the real Fastfile under a stub of the small slice of fastlane
# it touches, so there is nothing to keep in sync with the shipped source.

require "tmpdir"
require "fileutils"

module UI
  def self.important(msg)
    $log << "WARN #{msg}"
  end

  def self.success(msg)
    $log << "OK   #{msg}"
  end

  def self.user_error!(msg)
    raise(RuntimeError, msg)
  end
end

$lanes = {}

def default_platform(*)
  nil
end

def platform(_name)
  yield
end

def desc(*)
  nil
end

def lane(name, &block)
  $lanes[name] = block
end

def private_lane(name, &block)
  $lanes[name] = block
end

# Stands in for the fastlane action. An empty or never-used track raises in real
# life rather than returning [], so the stub does too.
def google_play_track_version_codes(package_name:, track:, json_key:)
  raise "Track '#{track}' not found." unless $play.key?(track)

  $play[track]
end

FASTFILE = File.expand_path("Fastfile", __dir__)
GRADLE_FILE = File.expand_path("../app/build.gradle.kts", __dir__)

# Deliberate: the input is this repo's own Fastfile, and evaluating it verbatim is
# the point — the spec tests the shipped source rather than a copy of its logic
# that could drift. The third argument makes __dir__ resolve inside the Fastfile.
eval(File.read(FASTFILE), TOPLEVEL_BINDING, FASTFILE)

ORIGINAL_GRADLE = File.read(GRADLE_FILE)
$failures = []

# Tags are read with git, so give each scenario a throwaway repo holding exactly
# the tags it describes. That keeps the spec independent of how much history the
# CI checkout happens to fetch.
def with_tags(tags)
  Dir.mktmpdir("version-bump-spec") do |dir|
    system("git", "init", "--quiet", dir, out: File::NULL, err: File::NULL)
    Dir.chdir(dir) do
      system("git", "-c", "user.email=spec@example.com", "-c", "user.name=spec",
             "commit", "--quiet", "--allow-empty", "-m", "base",
             out: File::NULL, err: File::NULL)
      tags.each { |tag| system("git", "tag", tag, out: File::NULL, err: File::NULL) }
      yield
    end
  end
end

def scenario(name, tree_code:, tree_name:, key:, play:, tags:,
             expect_code: nil, expect_name: nil, expect_error: nil)
  $log = []
  $play = play

  source = ORIGINAL_GRADLE
           .sub(/versionCode\s*=\s*\d+/, "versionCode = #{tree_code}")
           .sub(/versionName\s*=\s*"[^"]+"/, "versionName = \"#{tree_name}\"")
  File.write(GRADLE_FILE, source)
  key ? ENV["ANDROID_JSON_KEY_FILE"] = key : ENV.delete("ANDROID_JSON_KEY_FILE")

  begin
    with_tags(tags) { $lanes[:fetch_and_increment_build_number].call }

    if expect_error
      $failures << "#{name}: expected it to refuse (#{expect_error}), but it succeeded"
      return
    end

    written = File.read(GRADLE_FILE)
    code = written[/versionCode\s*=\s*(\d+)/, 1].to_i
    version_name = written[/versionName\s*=\s*"([^"]+)"/, 1]

    problems = []
    problems << "versionCode #{code}, expected #{expect_code}" if expect_code && code != expect_code
    problems << "versionName #{version_name}, expected #{expect_name}" if expect_name && version_name != expect_name

    if problems.empty?
      puts "PASS #{name} -> #{code} / #{version_name}"
    else
      $failures << "#{name}: #{problems.join('; ')}"
      puts "FAIL #{name}: #{problems.join('; ')}"
    end
  rescue RuntimeError => e
    if expect_error && e.message.include?(expect_error)
      puts "PASS #{name} -> refused, as it should: #{e.message}"
    else
      $failures << "#{name}: #{e.message}"
      puts "FAIL #{name}: #{e.message}"
    end
  end
ensure
  File.write(GRADLE_FILE, ORIGINAL_GRADLE)
end

# The regression itself: an in-flight deploy has published 380 but has not pushed
# its bump yet, so the tree still says 379. Reading the tree would pick 380 again.
scenario("stale tree while Play is ahead (the 2026-07-30 failure)",
         tree_code: 379, tree_name: "3.0.79", key: "/tmp/play-key.json",
         play: { "internal" => [380] }, tags: ["v3.0.79", "v3.0.80"],
         expect_code: 381, expect_name: "3.0.81")

scenario("tree in step with Play",
         tree_code: 380, tree_name: "3.0.80", key: "/tmp/play-key.json",
         play: { "internal" => [380] }, tags: ["v3.0.80"],
         expect_code: 381, expect_name: "3.0.81")

# The highest code need not live on internal — a promoted build sits further on.
scenario("highest code on another track",
         tree_code: 380, tree_name: "3.0.80", key: "/tmp/play-key.json",
         play: { "internal" => [380], "beta" => [391], "production" => [412] },
         tags: ["v3.0.80"], expect_code: 413, expect_name: "3.0.81")

# A bump that landed in git but was never published leaves the tree ahead.
scenario("tree ahead of Play",
         tree_code: 390, tree_name: "3.0.90", key: "/tmp/play-key.json",
         play: { "internal" => [380] }, tags: ["v3.0.80"],
         expect_code: 391, expect_name: "3.0.91")

scenario("no tags yet falls back to the tree's name",
         tree_code: 380, tree_name: "3.0.80", key: "/tmp/play-key.json",
         play: { "internal" => [380] }, tags: [],
         expect_code: 381, expect_name: "3.0.81")

# Tags sort by version, not lexically: v3.0.9 must not beat v3.0.80.
scenario("tags sort numerically",
         tree_code: 380, tree_name: "3.0.80", key: "/tmp/play-key.json",
         play: { "internal" => [380] }, tags: ["v3.0.9", "v3.0.80", "v3.0.100"],
         expect_code: 381, expect_name: "3.0.101")

# A local run has no service-account key: fall back to the tree, never fail.
scenario("no credential falls back to the tree",
         tree_code: 379, tree_name: "3.0.79", key: nil,
         play: {}, tags: ["v3.0.79"],
         expect_code: 380, expect_name: "3.0.80")

# But a credential that cannot read Play must not silently guess from the tree —
# guessing is what produces the duplicate code in the first place.
scenario("credential present but Play unreadable refuses to guess",
         tree_code: 379, tree_name: "3.0.79", key: "/tmp/play-key.json",
         play: {}, tags: ["v3.0.79"],
         expect_error: "refusing to guess")

if $failures.empty?
  puts "\nAll version-bump scenarios passed."
  exit 0
else
  puts "\n#{$failures.length} scenario(s) failed:"
  $failures.each { |f| puts "  - #{f}" }
  exit 1
end
