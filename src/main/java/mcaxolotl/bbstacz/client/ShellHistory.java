package mcaxolotl.bbstacz.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.animation.third.PlayerModelRenderState;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.renderer.item.GunItemTransforms;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import mcaxolotl.bbstacz.client.forms.TaczFormRenderer;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.ReplayBindings;
import mcaxolotl.bbstacz.recording.ShellSnapshot;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.renderers.mob.MobPoseApplier;
import mchorse.bbs_mod.mixin.client.LivingEntityRendererInvoker;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Read-only historical player + item transform walk. No rendering, animation tick or emission cache. */
public final class ShellHistory {
    private static final Map<TaczForm, Context> CONTEXTS = new WeakHashMap<>();
    private static final class Context {
        final TaczForm form = new TaczForm();
        final TaczFormRenderer renderer = (TaczFormRenderer) FormUtilsClient.getRenderer(form);
        final StubEntity source = new StubEntity(Minecraft.getInstance().level);
        final HistoricalScene scene = new HistoricalScene();
    }
    private ShellHistory() {}
    public static void clear() { CONTEXTS.clear(); }
    public static float emissionTick(float sampledTick, float ageMillis) { return sampledTick - ageMillis / 50f; }
    public static Matrix4f resolve(TaczForm form, ReplayBindings.Binding binding, float sampleTime, ItemStack gun, ShellSnapshot shell) {
        if (binding == null || binding.keyframes() == null || Minecraft.getInstance().level == null) return null;
        float born = emissionTick(sampleTime, shell.ageMillis());
        int tick = (int) Math.floor(born); float delta = born - tick;
        Context context = CONTEXTS.computeIfAbsent(form, f -> new Context());
        if (context.source.getWorld() != Minecraft.getInstance().level) { context = new Context(); CONTEXTS.put(form, context); }
        var source = context.source; var copy = context.form; var renderer = context.renderer;
        TaczForm base = binding.replay() != null && binding.replay().form.get() instanceof TaczForm replayForm ? replayForm : form;
        try {
            copy.fromData(base.toData());
            context.scene.sample(copy, source, binding, born, sampleTime);
            var frame = mcaxolotl.bbstacz.recording.FrameSampler.sample(binding.channel(), born);
            var proxy = renderer.visualPlayer(); if (proxy == null) return null;
            proxy.sync(source, frame, delta);
            PlayerRenderer playerRenderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(proxy);
            var model = playerRenderer.getModel();
            var display = TimelessAPI.getGunDisplay(gun).orElse(null);
            if (display == null || display.getGunModel() == null) return null;
            var gunModel = display.getGunModel(); var savedGun = NativePose.capture(gunModel);
            var savedCamera = new Quaternionf(gunModel.getCameraAnimationObject().rotationQuaternion);
            var savedParts = new IdentityHashMap<net.minecraft.client.model.geom.ModelPart, Transform>();
            try (var state = new PlayerModelRenderState(model); var scope = new NativeRenderScope(copy, source, proxy, delta, frame.humanoid(), true)) {
                model.prepareMobModel(proxy, proxy.walkAnimation.position(delta), proxy.walkAnimation.speed(delta), delta);
                model.setupAnim(proxy, proxy.walkAnimation.position(delta), proxy.walkAnimation.speed(delta), tick + delta,
                    proxy.yHeadRotO + (proxy.yHeadRot - proxy.yHeadRotO) * delta, proxy.getViewXRot(delta));
                NativePlayerPose.apply(model, frame.humanoid());
                MobPoseApplier.apply(renderer.getRig(), MobPoseApplier.merge(copy.pose.get(), copy.poseOverlay.get(), copy.additionalOverlays), savedParts);
                Matrix4f actor = context.scene.actor(source, binding.replay(), delta);
                PoseStack pose = new PoseStack(); pose.last().pose().set(actor);
                mchorse.bbs_mod.client.renderer.DeathPose.apply(pose, source.getDeathTime(), delta);
                pose.last().pose().mul(renderer.createEvaluatedTransform(delta).createMatrix());
                var offset = playerRenderer.getRenderOffset(proxy, delta); pose.translate(offset.x, offset.y, offset.z);
                var access = (LivingEntityRendererInvoker) playerRenderer;
                access.bbs$setupTransforms(proxy, pose, tick + delta, 0, delta);
                pose.scale(-1, -1, 1); access.bbs$scale(proxy, pose, delta); pose.translate(0, -1.501, 0);
                NativeBends.body(pose, frame.humanoid());
                model.translateToHand(HumanoidArm.RIGHT, pose);
                NativeBends.wrist(pose, frame.humanoid());
                pose.mulPose(Axis.XP.rotationDegrees(-90)); pose.mulPose(Axis.YP.rotationDegrees(180)); pose.translate(1 / 16f, 0.125, -0.625);
                NativeBends.item(pose, frame.humanoid());
                var baked = Minecraft.getInstance().getItemRenderer().getModel(gun, proxy.level(), proxy, 0);
                baked.getTransforms().getTransform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).apply(false, pose);
                pose.translate(-0.5, -0.5, -0.5);
                // TP items use their static gun pose, not FP recoil/inspection offsets in the shared model.
                gunModel.cleanAnimationTransform();
                GunItemTransforms.apply(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, display.getTransform().getScale(), gunModel, pose);
                var part = gunModel.getNode(shell.origin() == 0 ? "shell" : "shell_" + shell.origin());
                if (part == null) return null;
                var path = new ArrayList<BedrockPart>(); for (var p = part; p != null; p = p.getParent()) path.add(0, p);
                path.forEach(p -> p.translateAndRotateAndScale(pose));
                return new Matrix4f(pose.last().pose());
            } finally {
                MobPoseApplier.restore(savedParts); NativePose.apply(gunModel, savedGun); gunModel.getCameraAnimationObject().rotationQuaternion.set(savedCamera);
            }
        } finally { context.scene.close(); ReplayBindings.release(source); }
    }
}
