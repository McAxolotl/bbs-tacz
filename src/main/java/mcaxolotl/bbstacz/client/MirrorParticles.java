package mcaxolotl.bbstacz.client;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.firstperson.FirstPersonParticleSystem;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.particle.MuzzleParticleManager;
import com.tacz.guns.util.time.GunTime;
import mcaxolotl.bbstacz.mixin.client.MuzzleCaptureAccess;
import mcaxolotl.bbstacz.recording.ParticleSnapshot;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/** Render-free, isolated FP particle simulation for F5 recording. Never dispatches capture particles into the world. */
public final class MirrorParticles {
    private static FirstPersonParticleSystem system;
    private static final EnumMap<InteractionHand, List<Object>> pending = new EnumMap<>(InteractionHand.class);
    private static final EnumMap<InteractionHand, List<Object>> bound = new EnumMap<>(InteractionHand.class);
    private static final EnumMap<InteractionHand, Matrix4f> bases = new EnumMap<>(InteractionHand.class);
    private record WorldParticle(ParticleEmitterInstance emitter, ParticleInstance particle) {}
    private static final List<WorldParticle> world = new ArrayList<>();
    private static boolean active;
    private static long lastTime = -1;
    private static mchorse.bbs_mod.film.Recorder recorder;
    private static com.tacz.guns.client.resource.GunDisplayInstance display;
    private static net.minecraft.client.Camera camera;
    private static float lastTick = -1;
    private static long diverted;
    private MirrorParticles() {}
    public static boolean active() { return active; }
    public static net.minecraft.client.Camera camera() { return camera; }
    public static long divertedParticles() { return diverted; }
    /** A new take, model hierarchy or backwards seek never inherits a previous mirror simulation. */
    private static boolean prepare(ItemStack stack) {
        var nextRecorder = CommandJournal.recorder();
        var nextDisplay = com.tacz.guns.api.TimelessAPI.getGunDisplay(stack).orElse(null);
        if (nextRecorder == null || nextDisplay == null || Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            clear(); return false;
        }
        float tick = CommandJournal.time(nextRecorder);
        if (recorder != nextRecorder || display != nextDisplay || tick < lastTick) {
            clear(); recorder = nextRecorder; display = nextDisplay;
        }
        lastTick = tick;
        if (camera == null) camera = new net.minecraft.client.Camera();
        camera.setup(Minecraft.getInstance().level, Minecraft.getInstance().player, false, false, 0);
        return true;
    }
    public static void endFirstPersonGap() { if (!active && recorder != null) clear(); }
    private static FirstPersonParticleSystem system() { if (system == null) system = new FirstPersonParticleSystem(); return system; }
    private static final class Scope implements AutoCloseable {
        final EnumMap<InteractionHand,List<Object>> oldPending = new EnumMap<>(MuzzleCaptureAccess.pending());
        final EnumMap<InteractionHand,List<Object>> oldBound = new EnumMap<>(MuzzleCaptureAccess.bound());
        final EnumMap<InteractionHand,Matrix4f> oldBases = new EnumMap<>(MuzzleCaptureAccess.bases());
        final com.tacz.guns.client.resource.GunDisplayInstance oldDisplay = MuzzleCaptureAccess.lightDisplay();
        final long oldTime = MuzzleCaptureAccess.lightTime(); final int oldLight = MuzzleCaptureAccess.lightLevel();
        Scope() {
            if (active) throw new IllegalStateException("Nested mirror particle capture"); active = true;
            MuzzleCaptureAccess.pending().clear(); MuzzleCaptureAccess.pending().putAll(pending);
            MuzzleCaptureAccess.bound().clear(); MuzzleCaptureAccess.bound().putAll(bound);
            MuzzleCaptureAccess.bases().clear(); MuzzleCaptureAccess.bases().putAll(bases);
        }
        @Override public void close() {
            pending.clear(); pending.putAll(MuzzleCaptureAccess.pending()); bound.clear(); bound.putAll(MuzzleCaptureAccess.bound()); bases.clear(); bases.putAll(MuzzleCaptureAccess.bases());
            MuzzleCaptureAccess.pending().clear(); MuzzleCaptureAccess.pending().putAll(oldPending);
            MuzzleCaptureAccess.bound().clear(); MuzzleCaptureAccess.bound().putAll(oldBound);
            MuzzleCaptureAccess.bases().clear(); MuzzleCaptureAccess.bases().putAll(oldBases);
            MuzzleCaptureAccess.lightDisplay(oldDisplay); MuzzleCaptureAccess.lightTime(oldTime); MuzzleCaptureAccess.lightLevel(oldLight); active = false;
        }
    }
    public static void onShoot(ItemStack stack) {
        if (!prepare(stack)) return;
        try (var scope = new Scope()) { MuzzleParticleManager.enqueueShoot(stack); }
    }
    public static List<ParticleSnapshot> evaluate(BedrockGunModel model, PoseStack pose,
            List<com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData> animation) {
        if (!prepare(Minecraft.getInstance().player.getMainHandItem())) return List.of();
        long now = GunTime.nowMillis();
        float dt = lastTime < 0 ? 0 : Math.max(0, Math.min(0.1f, (now - lastTime) / 1000f)); lastTime = now;
        List<ParticleSnapshot> snapshots = new ArrayList<>();
        try (var scope = new Scope()) {
            system().tick(dt);
            for (var p : world) p.emitter.updateSingleParticle(p.particle, dt);
            world.removeIf(p -> !p.particle.alive || p.particle.age >= p.particle.maxLifetime);
            MuzzleParticleManager.captureBasePose(InteractionHand.MAIN_HAND, new Matrix4f());
            MuzzleParticleManager.spawnAndBind(system(), pose, model, InteractionHand.MAIN_HAND, animation);
            float pitch = Minecraft.getInstance().player.getXRot();
            Matrix4f camera = new Matrix4f().rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(Minecraft.getInstance().player.getYRot() + 180));
            for (var emitter : system().getEmitters()) {
                ParticleSnapshot snapshot = NativeParticles.snapshot(emitter, new Matrix4f(), new Matrix4f(), pitch, 0, camera);
                if (snapshot != null && snapshots.size() < 64) snapshots.add(snapshot);
            }
            var eye = Minecraft.getInstance().player.getEyePosition();
            Matrix4f worldView = new Matrix4f(camera).translate((float)-eye.x, (float)-eye.y, (float)-eye.z);
            for (var p : world) {
                if (snapshots.size() >= 64) break;
                var captured = NativeParticles.values(p.particle);
                snapshots.add(new ParticleSnapshot(p.emitter.getDefinition().getIdentifier().toString(), worldView.get(new float[16]),
                    new Matrix4f().get(new float[16]), camera.get(new float[16]), pitch, 0, 0, List.of(captured)));
            }
        }
        return snapshots;
    }
    public static void interceptWorldParticle(ParticleInstance p, ParticleEffectDefinition definition, ParticleEmitterInstance emitter) {
        if (world.size() >= 2048) return;
        ParticleInstance copy = NativeParticles.copy(p); copy.emitter = emitter; copy.updateComponents = p.updateComponents;
        diverted++;
        // Native delivery adds the camera's world position, then carried velocity for LOCAL velocity emitters.
        // Here that camera is the player's FP eye, never the F5 orbit/front camera.
        var player = Minecraft.getInstance().player;
        var eye = camera.getPosition(); copy.x += (float) eye.x; copy.y += (float) eye.y; copy.z += (float) eye.z;
        if (player != null && emitter.isLocalVelocity()) { copy.vx += (float)(player.getX()-player.xo)*20; copy.vy += (float)(player.getY()-player.yo)*20; copy.vz += (float)(player.getZ()-player.zo)*20; }
        world.add(new WorldParticle(emitter, copy));
    }
    public static int particleCount() { return (system == null ? 0 : system.getParticleCount()) + world.size(); }
    public static void clear() { if (system != null) system.clear(); system = null; pending.clear(); bound.clear(); bases.clear(); world.clear(); active = false; lastTime = -1; lastTick = -1; recorder = null; display = null; camera = null; diverted = 0; }
}
