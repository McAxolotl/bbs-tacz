package mcaxolotl.bbstacz.recording;

/** Pausing drops the render fraction, not the film tick; it must not destroy the native voice as a seek. */
public final class AudioCursor {
    private AudioCursor() {}

    public static boolean seeking(float previous, float time, boolean wasPlaying, boolean playing, boolean video) {
        if (!Float.isFinite(previous)) return false;
        boolean pauseFraction = wasPlaying && !playing && Math.floor(previous) == Math.floor(time);
        boolean backwards = time < previous - 0.001f && !pauseFraction;
        boolean forwardScrub = !video && time - previous > 2.1f;
        return backwards || forwardScrub;
    }
}
