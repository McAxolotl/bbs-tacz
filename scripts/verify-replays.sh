#!/usr/bin/env bash
# Use a real display, Java 17/21 and FFmpeg. Test worlds and probes never enter the published JAR.
set -euo pipefail
cd "$(dirname "$0")/.."
for executable in /usr/bin/ffmpeg /usr/bin/ffprobe; do
  [[ -x "$executable" ]] || { echo "Missing native media dependency: $executable" >&2; exit 1; }
done
command -v python3 >/dev/null
mkdir -p .gradle
exec 9>.gradle/bbs-tacz-native-review.lock
flock -n 9 || { echo 'Another native review suite is running.' >&2; exit 1; }
if pgrep -f '^[^ ]*/java .*net\.fabricmc\.devlaunchinjector\.Main' >/dev/null; then
  echo 'Stop the existing development Minecraft before recompiling/running the suite.' >&2
  exit 1
fi
# Keep logs outside build/ so clean does not erase evidence, and outside /tmp so they survive host cleanup.
log_dir="$PWD/run/native-review-logs/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$log_dir"
started=$(date +%s)
./gradlew clean build smokeClasses 2>&1 | tee "$log_dir/build.log"
# The manifest is the executable coverage list, not a hand-maintained PASS claim.
mapfile -t scenarios < <(python3 -c 'import json; print("\n".join(json.load(open("scripts/native-review-scenarios.json"))["standard"]))')
for scenario in "${scenarios[@]}"; do
  if pgrep -f '^[^ ]*/java .*net\.fabricmc\.devlaunchinjector\.Main' >/dev/null; then
    echo "A previous Minecraft process is still running; refusing to overlap $scenario." >&2
    exit 1
  fi
  minutes=$(python3 -c 'import json,sys; print(json.load(open("scripts/native-review-scenarios.json"))["timeout_minutes"].get(sys.argv[1],8))' "$scenario")
  timeout --kill-after=15s "${minutes}m" ./gradlew runClient -Preview="$scenario" 2>&1 | tee "$log_dir/$scenario.log"
done
timeout --kill-after=15s 8m ./gradlew runClient -Psmoke 2>&1 | tee "$log_dir/smoke.log"
mapfile -t reports < <(python3 -c 'import json; print("\n".join("run/bbs-tacz-review-"+name+"-video.txt" for name in json.load(open("scripts/native-review-scenarios.json"))["media"]))')
python3 scripts/audit-replay-media.py "${reports[@]}" --since "$started" --output "$log_dir/media-audit.json"
python3 scripts/audit-replay-coverage.py --since "$started" --output "$log_dir/coverage-audit.json"
printf '\nAll native scenarios, smoke and media checks passed. Evidence: %s\n' "$log_dir"
