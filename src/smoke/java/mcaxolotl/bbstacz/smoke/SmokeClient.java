package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.TimelessAPI;
import mcaxolotl.bbstacz.actions.TaczProjectileActionClip;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.client.forms.TaczFormRenderer;
import mcaxolotl.bbstacz.recording.FrameFactory;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;


import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import java.nio.file.Files;
import java.nio.file.Path;

/** Separate source set/mod; never packaged in the distributable. No user's worlds or settings are touched. */
public final class SmokeClient implements ClientModInitializer {
    private boolean ran;
    private int checks;
    private int phase;
    private FirstPersonSmoke firstPerson;
    private java.util.concurrent.CompletableFuture<Void> serverTests;
    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("bbstacz.smoke")) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ran || client.getOverlay() != null) return;
            if (phase == 0 && client.screen instanceof TitleScreen) {
                phase = 1;
                client.createWorldOpenFlows().createFreshLevel("bbs-tacz-smoke-" + System.currentTimeMillis(),
                    new net.minecraft.world.level.LevelSettings("BBS TACZ smoke", net.minecraft.world.level.GameType.CREATIVE, false,
                        net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(), net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                    new net.minecraft.world.level.levelgen.WorldOptions(0, false, false),
                    registry -> registry.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                        .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
                return;
            }
            if (phase == 0) return;
            try { if (runInWorld(client)) { ran = true; client.stop(); } }
            catch (Throwable e) { e.printStackTrace(); ran = true; client.stop(); }
        });
    }
    /** Same assertion core for development launch and explicitly requested Prism/OBS recording. */
    boolean runInWorld(net.minecraft.client.Minecraft client) {
        if (phase == 0) phase = 1;
        if (phase == 1 && client.player != null && !TimelessAPI.getAllClientGunIndex().isEmpty()) {
            phase = 2;
            serverTests = java.util.concurrent.CompletableFuture.runAsync(() -> {
                try { checks += WorldSmoke.run(client.getSingleplayerServer()); } catch (Exception e) { throw new RuntimeException(e); }
            }, client.getSingleplayerServer());
            return false;
        }
        if (phase == 3) {
            try {
                if (firstPerson.tick(client)) { checks += firstPerson.checks(); Files.writeString(Path.of("bbs-tacz-smoke.txt"), "PASS " + checks + "\n"); return true; }
            } catch (Throwable e) {
                e.printStackTrace();
                try { Files.writeString(Path.of("bbs-tacz-smoke.txt"), "FAIL " + checks + " " + e + "\n"); } catch (Exception ignored) {}
                throw new AssertionError("Native smoke failed after " + checks + " checks", e);
            }
            return false;
        }
        if (phase != 2 || serverTests == null || !serverTests.isDone() || !client.gameRenderer.getMainCamera().isInitialized() || !(client.player.getMainHandItem().getItem() instanceof com.tacz.guns.api.item.IGun)
            || !((dev.kosmx.playerAnim.impl.IAnimatedPlayer) client.player).playerAnimator_getAnimation().isActive()) return false;
        try {
            serverTests.join();
            ReplayKeyframes original = new ReplayKeyframes("frames");
            require(original instanceof TaczReplayChannels, "Common recording mixin did not apply");
            var channels = (TaczReplayChannels) original;
            channels.bbstacz$state().insert(0, FrameSnapshot.EMPTY);
            channels.bbstacz$commands().insert(0, "[{\"seq\":1,\"op\":\"trigger\",\"args\":[\"inspect\"]}]");
            ReplayKeyframes restored = new ReplayKeyframes("frames"); restored.fromData(original.toData());
            require(!((TaczReplayChannels) restored).bbstacz$state().isEmpty(), "Recording data was lost");
            require(((TaczReplayChannels) restored).bbstacz$commands().interpolate(0).contains("inspect"), "Command journal was lost");
            require(FrameFactory.INSTANCE.compare(FrameSnapshot.EMPTY, ((TaczReplayChannels) restored).bbstacz$state().interpolate(0)), "Pose round trip failed");
            require(!TimelessAPI.getAllClientGunIndex().isEmpty(), "TACZ gun assets did not load");
            require(TimelessAPI.getAllClientGunIndex().stream().allMatch(e -> e.getValue().getDefaultDisplay().getGunModel() != null), "Native gun models failed to initialize");
            TaczForm form = new TaczForm();
            var renderer = mchorse.bbs_mod.forms.FormUtilsClient.getRenderer(form);
            require(renderer instanceof TaczFormRenderer, "Dedicated form renderer was not registered");
            require(((TaczFormRenderer) renderer).visualPlayer() != null, "Native render proxy was not created");
            TaczProjectileActionClip clip = new TaczProjectileActionClip();
            clip.payload.get().putFloat("explosionRadius", 4); clip.payload.get().putBool("explosionDestroyBlock", true);
            TaczProjectileActionClip clone = new TaczProjectileActionClip(); clone.fromData(clip.toData());
            require(clone.payload.get().getFloat("explosionRadius") == 4 && clone.payload.get().getBool("explosionDestroyBlock"), "Projectile payload was lost");
            checks += NativeFormSmoke.run(client, form);
            firstPerson = new FirstPersonSmoke(client, form); phase = 3;
        } catch (Throwable e) {
            e.printStackTrace();
            try { Files.writeString(Path.of("bbs-tacz-smoke.txt"), "FAIL " + checks + " " + e + "\n"); } catch (Exception ignored) {}
            throw new AssertionError("Native smoke failed after " + checks + " checks", e);
        }
        return false;
    }
    int checks() { return checks; }
    private void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); checks++; }
}
