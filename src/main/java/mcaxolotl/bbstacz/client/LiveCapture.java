package mcaxolotl.bbstacz.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.util.time.GunTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mcaxolotl.bbstacz.mixin.client.MuzzleFlashAccess;
import mcaxolotl.bbstacz.mixin.client.ShellQueueAccess;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.ParticleSnapshot;
import mcaxolotl.bbstacz.recording.PhysicalCrawl;
import mcaxolotl.bbstacz.recording.PoseMath;
import mcaxolotl.bbstacz.recording.ShellSnapshot;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class LiveCapture {
    private static long shotId;
    private static float worldFov = -1, modelFov = -1;
    private static float[] handCamera = {0, 0, 0, 1};
    private static Pass pass;
    private static mchorse.bbs_mod.film.Recorder sampledRecorder;
    private static float sampledTick = -1;
    private static long sampledShot = -1;
    public static final class Pass {
        public final Matrix4f base;
        public final Matrix4f inverse;
        public final List<ParticleSnapshot> particles = new ArrayList<>();
        public Pass(PoseStack stack) { base = new Matrix4f(stack.last().pose()); inverse = new Matrix4f(base).invert(); }
    }
    private LiveCapture() {}
    public static Pass pass() { return pass; }
    public static void begin(PoseStack stack) { if (CommandJournal.recorder() != null && !FirstPersonPlayback.isRendering()) pass = new Pass(stack); }
    public static void end() { pass = null; }
    public static void onFire() {
        shotId++; CommandJournal.capture("fire", shotId);
        var client = Minecraft.getInstance(); var recorder = CommandJournal.recorder();
        if (recorder != null && client.player != null && !client.options.getCameraType().isFirstPerson()) {
            var frame = OffscreenCapture.evaluate(client.player, client.player.getMainHandItem(), CapturedShells.snapshot(null), shotId, 0, MuzzleFlashAccess.bbstacz$rotation());
            if (frame != null) ((TaczReplayChannels) recorder.keyframes).bbstacz$state().insert(CommandJournal.time(recorder), frame);
        }
    }
    public static void camera(Quaternionf q) { handCamera = new float[]{q.x, q.y, q.z, q.w}; }

    public static void fov(boolean world, double fov) {
        if (CommandJournal.recorder() != null && Double.isFinite(fov)) { if (world) worldFov = (float) fov; else modelFov = (float) fov; }
    }
    public static void captureModel(BedrockGunModel model, PoseStack stack, ItemStack gun, int light) {
        MirrorParticles.endFirstPersonGap();
        var recorder = CommandJournal.recorder();
        var player = Minecraft.getInstance().player;
        if (recorder == null || pass == null || player == null) return;
        float tick = CommandJournal.time(recorder);
        if (sampledRecorder == recorder && tick - sampledTick < 20f / mcaxolotl.bbstacz.config.TaczConfig.INSTANCE.recordingFps && sampledShot == shotId) return;
        float previous = sampledRecorder == recorder ? sampledTick : tick;
        String nbt = gun.save(new CompoundTag()).toString();
        // Tick recording can run ahead of the render sample (also when a film is stepped).
        // Once an actual FP pose exists, matching body-only placeholders must not split the FP timeline.
        var channel = ((TaczReplayChannels) recorder.keyframes).bbstacz$state();
        var recorded = channel.getKeyframes();
        for (int i = recorded.size() - 1; i >= 0; i--) {
            var key = recorded.get(i); if (key.getTick() < previous) break;
            if (!key.getValue().firstPerson() && key.getValue().gunStack().equals(nbt)) channel.remove(i);
        }
        sampledRecorder = recorder; sampledTick = tick; sampledShot = shotId;
        List<ShellSnapshot> shells = CapturedShells.snapshot(pass.inverse);
        long now = GunTime.nowMillis();
        float age = MuzzleFlashAccess.bbstacz$time() < 0 ? 1000 : Math.min(1_000_000, now - MuzzleFlashAccess.bbstacz$time());
        FrameSnapshot frame = new FrameSnapshot(nbt, true, NativePose.capture(model),
            ThirdPersonPose.sample(player, Minecraft.getInstance().getFrameTime()), new Matrix4f(pass.inverse).mul(stack.last().pose()).get(new float[16]),
            model.getRenderHand(), PhysicalCrawl.isCrawling(player), com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(Minecraft.getInstance().getFrameTime()),
            RefitTransform.getOpeningProgress(), shotId, age, MuzzleFlashAccess.bbstacz$rotation(), shells, handCamera, pass.particles, light, worldFov, modelFov);
        ((TaczReplayChannels) recorder.keyframes).bbstacz$state().insert(CommandJournal.time(recorder), frame);
    }
    public static void captureTick(TaczReplayChannels channels, int tick, IEntity entity) {
        if (!(entity instanceof MCEntity mc) || !(mc.getMcEntity() instanceof LivingEntity living)) return;
        ItemStack gun = living.getMainHandItem();
        FrameSnapshot frame = new FrameSnapshot(gun.save(new CompoundTag()).toString(), false, Map.of(), ThirdPersonPose.sample(living, 0),
            PoseMath.identity(), true, PhysicalCrawl.isCrawling(living), IGunOperator.fromLivingEntity(living).getSynAimingProgress(),
            0, -1, 1000, 0, living == Minecraft.getInstance().player ? CapturedShells.snapshot(null) : tickShells(gun), new float[]{0, 0, 0, 1}, List.of());
        // Do not put a non-FP fallback between two evaluated FP samples. Those samples carry
        // the complete native animation and already cover the interval by interpolation.
        var recorder = CommandJournal.recorder();
        if (recorder != null && channels == recorder.keyframes && Minecraft.getInstance().options.getCameraType().isFirstPerson() && !channels.bbstacz$state().isEmpty()) {
            boolean evaluated = sampledRecorder == recorder && sampledTick >= 0;
            if (evaluated && (com.tacz.guns.api.item.IGun.getIGunOrNull(gun) != null || com.tacz.guns.api.TimelessAPI.getGunDisplay(com.tacz.guns.api.client.other.KeepingItemRenderer.getRenderer().getCurrentItem()).isPresent())) return;
        }
        if (living == Minecraft.getInstance().player && !Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            float age = MuzzleFlashAccess.bbstacz$time() < 0 ? 1000 : Math.min(1_000_000, GunTime.nowMillis() - MuzzleFlashAccess.bbstacz$time());
            FrameSnapshot evaluated = OffscreenCapture.evaluate(Minecraft.getInstance().player, gun, CapturedShells.snapshot(null), shotId, age, MuzzleFlashAccess.bbstacz$rotation());
            if (evaluated != null) frame = evaluated;
        }
        channels.bbstacz$state().insert(tick, frame);
    }
    private static List<ShellSnapshot> tickShells(ItemStack gun) {
        var display = com.tacz.guns.api.TimelessAPI.getGunDisplay(gun).orElse(null);
        if (display == null || display.getGunModel() == null || display.getShellEjection() == null) return List.of();
        var shells = new ArrayList<ShellSnapshot>();
        long now = GunTime.nowMillis();
        for (int index = 0; index < 128; index++) {
            var renderer = display.getGunModel().getShellRender(index); if (renderer == null) break;
            for (ShellRender.Data data : ((ShellQueueAccess) renderer).bbstacz$shells()) {
                if (shells.size() >= 128 || data.epoch != GunTime.epoch() || now - data.timeStamp > display.getShellEjection().getLivingTime() * 1000) continue;
                shells.add(new ShellSnapshot(ShellIds.of(data), index, now - data.timeStamp,
                    new float[]{data.randomOffset.x, data.randomOffset.y, data.randomOffset.z}, PoseMath.identity()));
            }
        }
        return shells;
    }
    public static void clear() { CapturedShells.clear(); pass = null; shotId = 0; worldFov = -1; modelFov = -1; sampledRecorder = null; sampledTick = -1; sampledShot = -1; handCamera = new float[]{0, 0, 0, 1}; }
}
