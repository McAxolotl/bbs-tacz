package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.TimelessAPI;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

/** Separate native review entrypoint. Opens only a newly generated test world; no production hooks. */
public final class ReviewClient implements ClientModInitializer {
    private static boolean running;
    public static boolean isRunning() { return running; }
    public interface Scenario { boolean tick(Minecraft client, int tick) throws Exception; default int timeoutTicks() { return 1400; } }
    private boolean creating, finished, capturePrepared;
    private int captureHold;
    private final long startedAt = System.currentTimeMillis();
    private int tick;
    private Scenario scenario;
    @Override public void onInitializeClient() {
        String requested = System.getProperty("bbstacz.review");
        final String name = requested != null ? requested : ObsReviewSession.requestedScenario();
        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]*")) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (finished) return;
            try {
                if (creating && scenario != null && client.player == null) throw new AssertionError("Native review world disconnected before completion");
                if (client.getOverlay() != null) return;
                if (!creating && client.screen instanceof TitleScreen) {
                    creating = true; client.options.pauseOnLostFocus = false; client.options.renderDistance().set(8); client.options.simulationDistance().set(5);
                    client.createWorldOpenFlows().createFreshLevel("bbs-tacz-review-" + name + "-" + System.currentTimeMillis(),
                        new net.minecraft.world.level.LevelSettings("BBS TACZ review", net.minecraft.world.level.GameType.CREATIVE, false,
                            net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(), net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                        new net.minecraft.world.level.levelgen.WorldOptions(0, false, false),
                        registry -> registry.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                            .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
                    return;
                }
                if (!creating || client.player == null || !client.gameRenderer.getMainCamera().isInitialized() || TimelessAPI.getAllClientGunIndex().isEmpty()) return;
                if (scenario == null) {
                    if (ObsReviewSession.enabled() && !capturePrepared) {
                        capturePrepared = true;
                        org.lwjgl.glfw.GLFW.glfwSetWindowTitle(client.getWindow().getWindow(), "BBS TACZ - Prism verification");
                        client.getWindow().setWindowed(1280, 720);
                        ObsReviewSession.status("READY", 0, "Waiting for OBS window/audio capture handshake");
                        }
                    if (!ObsReviewSession.released()) return;
                    client.setScreen(null);
                    running = true;
                    scenario = (Scenario) Class.forName("mcaxolotl.bbstacz.smoke." + name).getDeclaredConstructor().newInstance();
                }
                if (captureHold > 0) {
                    if (--captureHold == 0) finish(client, name, "PASS");
                    return;
                }
                if (tick > scenario.timeoutTicks()) throw new AssertionError("Native review timed out");
                if (tick % 20 == 0) ObsReviewSession.status("RUNNING", tick, "Native assertions active; not yet passed");
                if (scenario.tick(client, tick++)) {
                    if (ObsReviewSession.enabled()) { captureHold = 40; ObsReviewSession.status("ASSERTIONS_DONE", tick, "Assertions passed; teardown still pending"); }
                    else finish(client, name, "PASS");
                }
            } catch (Throwable failure) { failure.printStackTrace(); finish(client, name, "FAIL " + failure); }
        });
    }
    private void finish(Minecraft client, String name, String result) {
        finished = true; running = false;
        try {
            // The result includes teardown. An early PASS hides crashes during borrowed-equipment / action cleanup.
            if (videoActive()) mchorse.bbs_mod.BBSModClient.getVideoRecorder().stopRecording();
            if (client.level != null) client.level.disconnect();
            client.clearLevel();
            Path crashes = Path.of("crash-reports");
            if (Files.isDirectory(crashes)) try (var files = Files.list(crashes)) {
                if (files.anyMatch(path -> path.toString().endsWith(".txt") && path.toFile().lastModified() >= startedAt))
                    result = "FAIL native crash report produced during validation";
            }
        } catch (Exception failure) { failure.printStackTrace(); result = "FAIL native teardown: " + failure; }
        try { Files.writeString(Path.of("bbs-tacz-review-" + name + ".txt"), result + "\n"); } catch (Exception e) { e.printStackTrace(); }
        ObsReviewSession.status(result.equals("PASS") ? "PASS" : "FAIL", tick, result);
        client.stop();
    }
    private boolean videoActive() { return mchorse.bbs_mod.BBSModClient.getVideoRecorder() != null && mchorse.bbs_mod.BBSModClient.getVideoRecorder().isRecording(); }
    public static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
