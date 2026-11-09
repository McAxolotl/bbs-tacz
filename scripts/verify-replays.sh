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
./gradlew clean build smokeClasses 2>&1 | tee "$log_dir/build.log"
scenarios=(
  ThirdPersonArmsReview FirstPersonSkinReview PlaybackAudioReview AudioRetakeReview
  NativeSoundExportReview NativeInterruptionExportReview HiddenPreviewReview CrawlOwnershipReview
  F4ClockReview F4BurstClockReview CtrlStartupReview ActionTailDurationReview ActionLifecycleReview HotkeyWorkflowReview
  F6ExportReview F6AudioExportReview F6NoDelayExportReview F6DraftExportReview F6CancelReview
  ProjectileReplayReview MultiGunWorkflowReview F4MultiGunExportReview
)
for scenario in "${scenarios[@]}"; do
  timeout --kill-after=15s 8m ./gradlew runClient -Preview="$scenario" 2>&1 | tee "$log_dir/$scenario.log"
done
timeout --kill-after=15s 8m ./gradlew runClient -Psmoke 2>&1 | tee "$log_dir/smoke.log"
python3 scripts/audit-replay-media.py --output "$log_dir/media-audit.json"
printf '\nAll native scenarios, smoke and media checks passed. Evidence: %s\n' "$log_dir"
