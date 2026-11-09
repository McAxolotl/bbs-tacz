package mcaxolotl.bbstacz.client;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.CameraStateCache;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.util.time.GunTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import mcaxolotl.bbstacz.recording.PoseMath;
import mcaxolotl.bbstacz.recording.ReplayBindings;
import mcaxolotl.bbstacz.recording.ShellSnapshot;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Shells are anchored once in WORLD space, not in the moving camera or a shared gun-display queue. */
public final class ThirdPersonShells {
    private record Pending(long id, int origin, long timestamp, float[] random) {}
    private static final class Shell {
        final ItemStack gun;
        Matrix4f world;
        final ShellSnapshot initial;
        final long born;
        final boolean replay;
        float age;
        Shell(ItemStack gun, Matrix4f world, ShellSnapshot data, boolean replay) {
            this.gun = gun.copy(); this.world = world; initial = data; born = GunTime.nowMillis() - (long) data.ageMillis(); this.replay = replay; age = data.ageMillis();
        }
    }
    private static final Map<LivingEntity, List<Pending>> PENDING = new WeakHashMap<>();
    private static final Map<IEntity, Map<String, Shell>> SHELLS = new WeakHashMap<>();
    private static final Map<IEntity, Float> LAST_TIME = new WeakHashMap<>();
    private static long remoteId;
    private ThirdPersonShells() {}
    public static void queue(LivingEntity owner, int origin, long id, long timestamp, Vector3f random) {
        if (owner == null) return;
        List<Pending> pending = PENDING.computeIfAbsent(owner, o -> new ArrayList<>());
        if (pending.size() >= 128) pending.remove(0);
        pending.add(new Pending(id, origin, timestamp, new float[]{random.x, random.y, random.z}));
    }
    public static void remoteFire(LivingEntity owner, ItemStack stack) {
        if (owner == Minecraft.getInstance().player) return; // Local ejection timing comes from Lua's actual popShellFrom call.
        TimelessAPI.getGunDisplay(stack).ifPresent(display -> {
            var ejection = display.getShellEjection(); if (ejection == null) return;
            var random = ejection.getRandomVelocity();
            queue(owner, 0, --remoteId, GunTime.nowMillis(), new Vector3f(owner.getRandom().nextFloat() * random.x,
                owner.getRandom().nextFloat() * random.y, owner.getRandom().nextFloat() * random.z));
        });
    }
    public static void capture(BedrockGunModel model, PoseStack pose, ItemStack gun) {
        NativeRenderScope scope = NativeRenderScope.current();
        if (scope == null || scope.matrices || com.tacz.guns.compat.iris.IrisCompat.isRenderShadow()) return;
        if (scope.frame != null) return; // Replay lifetimes are updated by the world pass, not by visible item draws.
        Map<String, Shell> cache = SHELLS.computeIfAbsent(scope.entity, e -> new LinkedHashMap<>());
        if (scope.entity instanceof MCEntity mc && mc.getMcEntity() instanceof LivingEntity owner) {
            List<Pending> pending = PENDING.remove(owner);
            if (pending != null) for (Pending emission : pending) {
                ShellSnapshot shell = new ShellSnapshot(emission.id, emission.origin, GunTime.nowMillis() - emission.timestamp, emission.random, PoseMath.identity());
                cache.put(emission.id + ":" + emission.origin, new Shell(gun, emissionWorld(model, pose, emission.origin), shell, false));
            }
        }
        while (cache.size() > 128) cache.remove(cache.keySet().iterator().next());
    }
    /** Called even for culled actors and actors that have put the gun away. Seeking is idempotent. */
    public static void updateReplay(IEntity entity, ReplayBindings.Binding binding, float time) {
        if (!(entity.getForm() instanceof mcaxolotl.bbstacz.forms.TaczForm form)) return;
        Map<String, Shell> cache = SHELLS.computeIfAbsent(entity, e -> new LinkedHashMap<>());
        Float last = LAST_TIME.put(entity, time); if (last != null && time < last) cache.clear();
        var frame = mcaxolotl.bbstacz.recording.FrameSampler.sample(binding.channel(), time);
        var visible = new java.util.HashSet<String>();
        for (var shell : frame.shells()) {
            String key = shell.id() + ":" + shell.origin(); visible.add(key);
            float born = ShellHistory.emissionTick(time, shell.ageMillis());
            String nbt = shell.gunStack().isEmpty() ? mcaxolotl.bbstacz.recording.FrameSampler.sample(binding.channel(), born).gunStack() : shell.gunStack();
            ItemStack gun;
            try { gun = ItemStack.of(net.minecraft.nbt.TagParser.parseTag(nbt)); } catch (Exception invalid) { continue; }
            var display = TimelessAPI.getGunDisplay(gun).orElse(null);
            if (display == null || display.getShellEjection() == null || shell.ageMillis() > display.getShellEjection().getLivingTime() * 1000) continue;
            Matrix4f historical = ShellHistory.resolve(form, binding, time, gun, shell);
            if (historical == null) continue; // Never fall back to today's wrist for a historical emission.
            Shell old = cache.get(key);
            if (old == null || !ItemStack.matches(old.gun, gun)) cache.put(key, new Shell(gun, historical, shell, true));
            else { old.age = shell.ageMillis(); old.world = historical; }
        }
        cache.entrySet().removeIf(entry -> entry.getValue().replay && !visible.contains(entry.getKey()));
    }
    /** Diagnostics and regression tests; callers receive copies, never mutable cached matrices. */
    public static List<ShellSnapshot> snapshots(IEntity entity) {
        var cache = SHELLS.get(entity); if (cache == null) return List.of();
        return cache.values().stream().map(shell -> new ShellSnapshot(shell.initial.id(), shell.initial.origin(), shell.age,
            shell.initial.randomVelocity(), shell.world.get(new float[16]), shell.gun.save(new net.minecraft.nbt.CompoundTag()).toString())).toList();
    }
    private static Matrix4f emissionWorld(BedrockGunModel model, PoseStack pose, int origin) {
        BedrockPart part = model.getNode(origin == 0 ? "shell" : "shell_" + origin);
        List<BedrockPart> path = new ArrayList<>();
        for (BedrockPart p = part; p != null; p = p.getParent()) path.add(0, p);
        pose.pushPose();
        try {
            path.forEach(p -> p.translateAndRotateAndScale(pose));
            return toWorld(pose.last().pose());
        } finally { pose.popPose(); }
    }
    private static Matrix4f view() {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        return new Matrix4f().rotateZ(CameraStateCache.getCameraRollRadians()).rotateX((float) Math.toRadians(camera.getXRot()))
            .rotateY((float) Math.toRadians(camera.getYRot() + 180));
    }
    private static Matrix4f toWorld(Matrix4f matrix) {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera(); var p = camera.getPosition();
        return new Matrix4f().translation((float) p.x, (float) p.y, (float) p.z).mul(view().invert()).mul(matrix);
    }
    public static void render(WorldRenderContext context) {
        if (com.tacz.guns.compat.iris.IrisCompat.isRenderShadow()) return;
        for (var entry : ReplayBindings.snapshot().entrySet()) updateReplay(entry.getKey(), entry.getValue(), ReplayClock.time(entry.getKey(), entry.getValue(), context.tickDelta()));
        var camera = context.camera(); var position = camera.getPosition();
        Matrix4f worldToView = view().translate((float) -position.x, (float) -position.y, (float) -position.z);
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        for (var cache : SHELLS.values()) {
            cache.entrySet().removeIf(entry -> {
                Shell shell = entry.getValue();
                float age = shell.replay ? shell.age : GunTime.nowMillis() - shell.born;
                var display = TimelessAPI.getGunDisplay(shell.gun).orElse(null);
                if (display == null || display.getShellEjection() == null || age > display.getShellEjection().getLivingTime() * 1000) return true;
                ShellSnapshot draw = new ShellSnapshot(shell.initial.id(), shell.initial.origin(), age, shell.initial.randomVelocity(), shell.world.get(new float[16]));
                NativeShells.render(display, shell.gun, draw, worldToView, buffers, 15728880, 0, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
                return false;
            });
        }
        buffers.endBatch();
    }
    public static void release(IEntity entity) { SHELLS.remove(entity); LAST_TIME.remove(entity); }
    public static void clear() { PENDING.clear(); SHELLS.clear(); LAST_TIME.clear(); remoteId = 0; }
}
