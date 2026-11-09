package mcaxolotl.bbstacz.smoke;

/** Disabling the cosmetic video delay must still await the asynchronous world playback before recording. */
public final class F6NoDelayExportReview extends F6ExportReview {
    @Override protected float warmupDelay() { return 0; }
}
