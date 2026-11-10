package mcaxolotl.bbstacz.smoke;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/** Explicit, removable launcher test companion. No hook is packaged in the release addon. */
final class ObsReviewSession {
    private static final Path REQUEST = Path.of("bbs-tacz-obs-review.request");
    private static boolean enabled;
    private static String scenario;
    private static long lastImage;
    static String requestedScenario() {
        if (!Files.isRegularFile(REQUEST)) return null;
        try {
            scenario = Files.readString(REQUEST).trim();
            if (!scenario.matches("[A-Za-z][A-Za-z0-9_]*")) throw new IllegalArgumentException("Invalid OBS review scenario");
            // A request is consumed once; restarting Prism must not silently replay a long test.
            Files.move(REQUEST, Path.of("bbs-tacz-obs-review.consumed"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            Files.deleteIfExists(Path.of("bbs-tacz-obs-review.go"));
            enabled = true;
            status("STARTING", 0, "Dedicated newly created test world only");
            return scenario;
        } catch (Exception e) { throw new IllegalStateException("Cannot read explicit OBS review request", e); }
    }
    static boolean enabled() { return enabled; }
    static boolean released() { return !enabled || Files.isRegularFile(Path.of("bbs-tacz-obs-review.go")); }
    static void status(String state, int tick, String detail) {
        if (!enabled) return;
        try {
            Path temporary = Path.of("bbs-tacz-obs-status.json.new");
            Files.writeString(temporary, new Gson().toJson(Map.of("scenario",scenario,"state",state,"tick",tick,"detail",detail,"time",System.currentTimeMillis())));
            Files.move(temporary, Path.of("bbs-tacz-obs-status.json"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) { throw new IllegalStateException("OBS review status write failed", e); }
    }
    static void evidence(NativeImage image) {
        if (!enabled || System.currentTimeMillis()-lastImage < 400) return;
        lastImage = System.currentTimeMillis();
        Path temporary = Path.of("bbs-tacz-obs-evidence.new.png");
        try {
            image.writeToFile(temporary);
            Files.move(temporary, Path.of("bbs-tacz-obs-evidence.png"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) { throw new IllegalStateException("Cannot publish GPU evidence", e); }
    }
}
