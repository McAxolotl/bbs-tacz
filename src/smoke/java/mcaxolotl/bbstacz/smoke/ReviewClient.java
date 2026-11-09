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
    public interface Scenario { boolean tick(Minecraft client, int tick) throws Exception; }
    private boolean creating, finished;
    private int tick;
    private Scenario scenario;
    @Override public void onInitializeClient() {
        String name = System.getProperty("bbstacz.review");
        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]*")) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (finished || client.getOverlay() != null) return;
            try {
                if (!creating && client.screen instanceof TitleScreen) {
                    creating = true; client.options.pauseOnLostFocus = false;
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
                    client.setScreen(null);
                    scenario = (Scenario) Class.forName("mcaxolotl.bbstacz.smoke." + name).getDeclaredConstructor().newInstance();
                }
                if (tick > 1400) throw new AssertionError("Native review timed out");
                if (scenario.tick(client, tick++)) finish(client, name, "PASS");
            } catch (Throwable failure) { failure.printStackTrace(); finish(client, name, "FAIL " + failure); }
        });
    }
    private void finish(Minecraft client, String name, String result) {
        finished = true;
        try { Files.writeString(Path.of("bbs-tacz-review-" + name + ".txt"), result + "\n"); } catch (Exception e) { e.printStackTrace(); }
        client.stop();
    }
    public static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
