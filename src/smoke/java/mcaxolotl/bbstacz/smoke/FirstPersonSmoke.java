package mcaxolotl.bbstacz.smoke;

import cn.sh1rocu.tacz.api.LogicalSide;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FirstPersonFilmController;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.film.replays.Replay;
import mcaxolotl.bbstacz.forms.TaczForm;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import java.lang.reflect.Field;

/** Exercises the real GPU capture/playback paths without sending test films to a user's repository. */
final class FirstPersonSmoke {
    private final Field recorderField;
    private final Recorder recorder;
    private final Film film;
    private final Replay replay;
    private int tick;
    private int checks;
    int checks() { return checks; }
    private long beforePlayback;
    private long beforeArms;
    private int waitingForHands;
    private FirstPersonFilmController playback;
    FirstPersonSmoke(Minecraft client, TaczForm form) throws Exception {
        client.options.pauseOnLostFocus = false;
        client.setScreen(null);
        client.options.setCameraType(CameraType.FIRST_PERSON);
        Morph.getMorph(client.player).setForm(form);
        film = new Film(); film.setId("bbstacz-gpu-smoke"); replay = new Replay("smoke_player");
        replay.form.set(form); replay.fp.set(true); film.replays.add(replay);
        recorder = new Recorder(film, form, 0, 0); recorder.countdown = 0;
        recorderField = mchorse.bbs_mod.film.Films.class.getDeclaredField("recorder"); recorderField.setAccessible(true);
        recorderField.set(BBSModClient.getFilms(), recorder);
    }
    boolean tick(Minecraft client) throws Exception {
        var gun = client.player.getMainHandItem();
        var state = GunItemRendererWrapper.INSTANCE.get().getStateMachine(gun);
        if (tick < 100) {
            recorder.tick = tick;
            recorder.keyframes.record(tick, Morph.getMorph(client.player).entity, null);
            if (tick == 90) {
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                var renew = TimelessAPI.getAllClientGunIndex().stream().filter(e -> e.getValue().getDefaultDisplay().getMuzzleFlash() != null && e.getValue().getDefaultDisplay().getMuzzleFlash().isRenew()).findFirst().orElseThrow();
                gun = com.tacz.guns.api.item.builder.GunItemBuilder.create().setId(renew.getKey()).setAmmoCount(30).setAmmoInBarrel(true).build();
                client.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, gun);
                int before = particleEngineSize(client);
                mcaxolotl.bbstacz.client.MirrorParticles.onShoot(gun);
                var mirror = mcaxolotl.bbstacz.client.OffscreenCapture.evaluate(client.player, gun, java.util.List.of(), 1000, 0, 0);
                require(mirror != null && !mirror.particles().isEmpty(), "F5 mirror did not evaluate native flash particles");
                require(mcaxolotl.bbstacz.client.MirrorParticles.divertedParticles() > 0 && particleEngineSize(client) == before,
                    "F5 mirror dispatched duplicate capture smoke into the actual particle engine");
            }
            if (tick == 96) client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            if (tick == 99) client.options.setCameraType(CameraType.FIRST_PERSON);
            if (tick >= 91 && tick <= 98) {
                state = GunItemRendererWrapper.INSTANCE.get().getStateMachine(gun);
                if (state != null) state.trigger("shoot");
                GunFireEvent.CALLBACK.invoker().post(new GunFireEvent(client.player, gun, LogicalSide.CLIENT));
            }
            if (state != null) switch (tick) {
                case 4, 56 -> state.trigger("inspect");
                case 8, 60, 70 -> client.setScreen(new GunRefitScreen());
                case 16 -> RefitTransform.changeRefitScreenView(AttachmentType.SCOPE);
                case 24 -> RefitTransform.changeRefitScreenView(AttachmentType.MUZZLE);
                case 32, 66, 86 -> { if (client.screen != null) client.screen.onClose(); }
                case 36 -> state.trigger("reload");
                case 44 -> state.trigger("bayonet_push");
                case 48 -> {
                    state.trigger("shoot");
                    GunFireEvent.CALLBACK.invoker().post(new GunFireEvent(client.player, gun, LogicalSide.CLIENT));
                }
                case 52, 53, 54, 55 -> GunFireEvent.CALLBACK.invoker().post(new GunFireEvent(client.player, gun, LogicalSide.CLIENT));
                case 78 -> RefitTransform.changeRefitScreenView(AttachmentType.GRIP);
            }
        } else if (tick == 100) {
            recorderField.set(BBSModClient.getFilms(), null);
            var channels = (TaczReplayChannels) recorder.keyframes;
            require(channels.bbstacz$state().getKeyframes().stream().filter(k -> k.getValue().firstPerson()).count() > 20,
                "Real FP capture missing; paused=" + client.isPaused() + " gun=" + gun + " stateKeys=" + channels.bbstacz$state().getKeyframes().size() + " commands=" + channels.bbstacz$commands().getKeyframes().size());
            require(channels.bbstacz$commands().getKeyframes().size() > 10, "Nested native animation commands were not recorded");
            channels.bbstacz$state().simplify(); channels.bbstacz$commands().simplify();
            replay.keyframes.fromData(recorder.keyframes.toData());
            var restored = ((TaczReplayChannels) replay.keyframes).bbstacz$state();
            require(restored.getKeyframes().stream().anyMatch(k -> !k.getValue().gunBones().isEmpty()), "Gun bones did not survive save/stop/read");
            require(restored.getKeyframes().stream().anyMatch(k -> k.getValue().shotAgeMillis() < 50), "Actual flash missing; minAge=" + restored.getKeyframes().stream().filter(k -> k.getValue().firstPerson()).mapToDouble(k -> k.getValue().shotAgeMillis()).min().orElse(-1) + " shot=" + restored.getKeyframes().stream().mapToLong(k -> k.getValue().shotId()).max().orElse(-1));
            require(restored.getKeyframes().stream().anyMatch(k -> k.getTick() >= 90 && k.getTick() < 99 && !k.getValue().particles().isEmpty()), "F5 flash/smoke snapshots did not survive save/read");
            System.out.println("[BBS TACZ smoke] fp/hands keys=" + restored.getKeyframes().stream().filter(k -> k.getValue().firstPerson()).count() + "/" + restored.getKeyframes().stream().filter(k -> k.getValue().firstPerson() && k.getValue().renderHands()).count());
            float firstFp = restored.getKeyframes().stream().filter(k -> k.getValue().firstPerson()).map(k -> k.getTick()).min(Float::compare).orElseThrow();
            require(restored.getKeyframes().stream().noneMatch(k -> k.getTick() > firstFp && !k.getValue().firstPerson()), "Body-only fallback keys broke the evaluated FP timeline");
            System.out.println("[BBS TACZ smoke] initial keys=" + restored.getKeyframes().stream().limit(30).map(k -> k.getTick()+":"+k.getValue().firstPerson()+":"+k.getValue().renderHands()+":"+k.getValue().refit()).toList());
            playback = new FirstPersonFilmController(film); playback.tick = java.util.stream.IntStream.range(0, 90).filter(t -> { var f = mcaxolotl.bbstacz.recording.FrameSampler.sample(restored, t); return f.firstPerson() && f.renderHands(); }).findFirst().orElseThrow(); playback.paused = true;
            FirstPersonPlayback.watch(playback); beforePlayback = FirstPersonPlayback.renderedFrames(); beforeArms = MountProbe.arms; MountProbe.neutral = true;
            client.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY); Morph.getMorph(client.player).setForm(null);
            System.out.println("[BBS TACZ smoke] starting paused replay: target=" + (FirstPersonPlayback.target(0) != null) + " chosen=" + playback.tick + " fp at56=" + restored.interpolate(56, FrameSnapshot.EMPTY).firstPerson() + " fp at60=" + restored.interpolate(60, FrameSnapshot.EMPTY).firstPerson() + " currentGun=" + client.player.getMainHandItem());
        } else if (tick == 108) {
            if (MountProbe.arms <= beforeArms && waitingForHands++ < 200) return false;
            require(MountProbe.arms > beforeArms && MountProbe.neutral, "Replay hands missing/non-neutral: arms=" + (MountProbe.arms-beforeArms) + " neutral=" + MountProbe.neutral + " hands=" + (FirstPersonPlayback.target(0) == null ? "missing" : FirstPersonPlayback.target(0).frame().renderHands()));
            playback.tick = 60;
            var target = FirstPersonPlayback.target(0);
            require(target != null, "FP frame was lost at the native refit boundary");
            var fov = client.gameRenderer.getClass().getDeclaredMethod(net.fabricmc.loader.api.FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_757", "method_3196", "(Lnet/minecraft/class_4184;FZ)D"), net.minecraft.client.Camera.class, float.class, boolean.class); fov.setAccessible(true);
            double actual = (double) fov.invoke(client.gameRenderer, client.gameRenderer.getMainCamera(), 0f, true);
            require(target.frame().worldFov() <= 0 || Math.abs(actual-target.frame().worldFov()) < 1e-4, "Native FOV modifiers overwrote the recorded FOV");
        } else if (tick >= 112) {
            if (FirstPersonPlayback.renderedFrames() <= beforePlayback + 2 && tick < 240) { tick++; return false; }
            System.out.println("[BBS TACZ smoke] playback tick=" + playback.getTick() + " camera=" + client.options.getCameraType() + " target=" + (FirstPersonPlayback.target(0) != null) + " renders=" + (FirstPersonPlayback.renderedFrames()-beforePlayback));
            require(FirstPersonPlayback.renderedFrames() > beforePlayback + 2, "Native first-person replay did not render");
            var first = FirstPersonPlayback.target(0.2f); var second = FirstPersonPlayback.target(0.8f);
            require(first != null && second != null && first.tick() == second.tick(), "Paused playback still advances with render delta");
            FirstPersonPlayback.stop(playback); playback.shutdown();
            return true;
        }
        tick++;
        return false;
    }
    private static int particleEngineSize(Minecraft client) throws Exception {
        int count = 0;
        for (Field field : client.particleEngine.getClass().getDeclaredFields()) {
            if (java.util.Collection.class.isAssignableFrom(field.getType())) { field.setAccessible(true); var value = (java.util.Collection<?>) field.get(client.particleEngine); if (value != null) count += value.size(); }
            else if (java.util.Map.class.isAssignableFrom(field.getType())) { field.setAccessible(true); var value = (java.util.Map<?,?>) field.get(client.particleEngine); if (value != null) for (var queue : value.values()) if (queue instanceof java.util.Collection<?> particles) count += particles.size(); }
        }
        return count;
    }
    void require(boolean value, String message) { if (!value) throw new AssertionError(message); checks++; }
}
