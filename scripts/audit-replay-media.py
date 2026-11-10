#!/usr/bin/env python3
"""Decode the latest native-review MP4s; a codec label alone does not prove audible, intact media.

Sound provenance is asserted by the Java scenarios (TACZ resources and no generated AudioClip).
This separate audit checks the actual encoded video/audio, not only BBS's capture objects.
"""
import argparse
from array import array
from fractions import Fraction
import json
import math
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SCENARIOS = ("F4MultiGunExportReview", "MultiGunWorkflowReview", "F6AudioExportReview")


def run(*args: str) -> bytes:
    return subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, timeout=180).stdout


def audit(report: Path, since: float | None = None) -> dict:
    assert since is None or report.stat().st_mtime >= since, f"Stale export report: {report}"
    video = Path(report.read_text().strip())
    assert video.is_absolute() and video.is_file(), f"Missing native export from {report}: {video}"
    data = json.loads(run("/usr/bin/ffprobe", "-v", "error", "-show_streams", "-show_format", "-of", "json", str(video)))
    picture = next(s for s in data["streams"] if s["codec_type"] == "video")
    sound = next(s for s in data["streams"] if s["codec_type"] == "audio")
    assert picture["codec_name"] == "h264" and sound["codec_name"] == "aac", f"Unexpected codecs: {video}"
    fps = float(Fraction(picture["avg_frame_rate"]))
    duration = float(picture["duration"])
    frames = int(picture["nb_frames"])
    assert math.isclose(fps, 30, abs_tol=0.001), f"Wrong native-test export rate: {fps}"
    assert duration > 0 and frames > 0 and abs(frames - duration * fps) <= 1, f"Invalid frame duration: {video}"
    assert abs(float(sound["duration"]) - duration) <= 0.1, f"Audio/video duration mismatch: {video}"
    # Decode the complete picture stream; fail on bitstream errors rather than accepting a valid MP4 header.
    run("/usr/bin/ffmpeg", "-v", "error", "-xerror", "-i", str(video), "-map", "0:v:0", "-an", "-f", "null", "-")
    samples = array("h", run("/usr/bin/ffmpeg", "-v", "error", "-xerror", "-i", str(video), "-map", "0:a:0", "-vn", "-ac", "1", "-ar", "16000", "-f", "s16le", "-"))
    if sys.byteorder != "little":
        samples.byteswap()
    nonzero = sum(sample != 0 for sample in samples)
    peak = max((abs(sample) for sample in samples), default=0) / 32768
    rms = math.sqrt(sum(sample * sample for sample in samples) / max(1, len(samples))) / 32768
    assert len(samples) > 1600 and nonzero > 1600 and peak > 0.005, f"Export has no meaningful audible track: {video}"
    body_samples = []
    if report.name in ("bbs-tacz-review-MovingThirdPersonWorkflowReview-video.txt", "bbs-tacz-review-RecordedThirdPersonF6Review-video.txt"):
        # These two fixtures deliberately use the default wide Steve skin. The cyan shirt
        # below the horizon is an output-image check, not merely an off-screen draw count.
        for at in (5, 15, 25, 35, 45, 55):
            pixels = run("/usr/bin/ffmpeg", "-v", "error", "-ss", str(at), "-i", str(video), "-vf", "scale=320:180", "-frames:v", "1", "-f", "rawvideo", "-pix_fmt", "rgb24", "-")
            assert len(pixels) == 320 * 180 * 3, f"Missing TP output frame at {at}s"
            cyan = 0
            for y in range(72, 151):
                for x in range(96, 225):
                    offset = (y * 320 + x) * 3
                    r, g, b = pixels[offset:offset+3]
                    if r < 100 and g > max(20, r * 1.5) and b > r * 1.4 and 0.75 < g / max(1, b) < 1.5:
                        cyan += 1
            assert cyan >= 4, f"TP actor not visibly framed at {at}s (shirt pixels={cyan}): {video}"
            body_samples.append({"second": at, "visible_shirt_pixels": cyan})
    return {
        "third_person_body_samples": body_samples,
        "report": str(report.resolve()), "video": str(video), "video_codec": picture["codec_name"],
        "audio_codec": sound["codec_name"], "fps": fps, "frames": frames, "duration_seconds": duration,
        "audio_duration_seconds": float(sound["duration"]), "decoded_samples_16khz_mono": len(samples),
        "nonzero_samples": nonzero, "peak": peak, "rms": rms, "full_video_decode": "PASS",
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("reports", nargs="*", type=Path, help="Native-review *-video.txt reports (default: F4/F6 multi-gun and F6 skin/audio)")
    parser.add_argument("--output", type=Path, help="Optional JSON evidence file")
    parser.add_argument("--since", type=float, help="Reject export reports older than this Unix epoch")
    args = parser.parse_args()
    reports = args.reports or [ROOT / "run" / f"bbs-tacz-review-{name}-video.txt" for name in DEFAULT_SCENARIOS]
    results = {"status": "PASS", "media": [audit(report, args.since) for report in reports]}
    text = json.dumps(results, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text)
    print(text, end="")


if __name__ == "__main__":
    main()
