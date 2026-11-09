package mcaxolotl.bbstacz.client.forms;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.animation.third.PlayerModelRenderState;
import com.tacz.guns.client.animation.third.AnimationRenderContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.client.NativeRenderScope;
import mcaxolotl.bbstacz.client.ThirdPersonPose;
import mcaxolotl.bbstacz.recording.PartPose;
import mcaxolotl.bbstacz.recording.ReplayBindings;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.IBoneHierarchy;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.mob.MobRenderContext;
import mchorse.bbs_mod.forms.renderers.mob.MobPoseApplier;
import mchorse.bbs_mod.forms.renderers.mob.MobPickerVertexConsumer;
import mchorse.bbs_mod.forms.renderers.mob.MobRig;
import mchorse.bbs_mod.forms.renderers.mob.MobRigs;
import mchorse.bbs_mod.forms.renderers.mob.MobRigMatrices;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.pose.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Standalone native player form: native layers draw weapons, this class owns actor state and timeline identity. */
public final class TaczFormRenderer extends FormRenderer<TaczForm> implements ITickable {
    private final GameProfile profile = new GameProfile(UUID.randomUUID(), "BBS_TACZ");
    private TaczVisualPlayer player;
    private IEntity actor;
    private final MatrixCache bones = new MatrixCache();
    public TaczFormRenderer(TaczForm form) { super(form); }
    public TaczVisualPlayer visualPlayer() {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        if (actor != null && actor.getWorld() != level) actor = null;
        if (player == null || player.level() != level) player = new TaczVisualPlayer(level, profile, form.slim.get());
        player.slim = form.slim.get();
        player.selectedSkin(mcaxolotl.bbstacz.client.SkinTextureBridge.skin(form.texture.get()));
        return player;
    }
    public MobRig getRig() {
        var player = visualPlayer(); if (player == null) return null;
        return MobRigs.of(renderer().getModel());
    }
    private PlayerRenderer renderer() { return (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player); }
    @Override public List<String> getBones() { var rig = getRig(); return rig == null ? List.of() : rig.getGroupKeysInHierarchyOrder(); }
    @Override public IBoneHierarchy getBoneHierarchy() { return getRig(); }
    private Pose authoredPose() { form.syncOverlayTracks(); return MobPoseApplier.merge(form.pose.get(), form.poseOverlay.get(), form.additionalOverlays); }
    private Map<String, PartPose> evaluated(IEntity source, float delta) {
        var frame = ReplayBindings.sample(source, delta);
        if (frame != null && !frame.humanoid().isEmpty()) return frame.humanoid();
        LivingEntity live = !mcaxolotl.bbstacz.client.ActorAnimations.owns(source) && source instanceof MCEntity mc && mc.getMcEntity() instanceof LivingEntity l ? l : player;
        return ThirdPersonPose.sample(live, delta, form.slim.get());
    }
    @Override public void tick(IEntity entity) {
        actor = entity; var visual = visualPlayer(); if (visual != null) { visual.sync(entity, ReplayBindings.sample(entity, 0)); mcaxolotl.bbstacz.client.ActorAnimations.tick(entity, visual); }
    }
    @Override protected void updateStencilMap(FormRenderingContext context) {
        context.stencilMap.addPicking(form, ""); var rig = getRig();
        if (rig != null) for (var part : rig.ordered()) context.stencilMap.addPicking(form, rig.name(part));
    }
    @Override protected void render3D(FormRenderingContext context) {
        actor = context.entity; draw(context.entity, context.stack, context.light, context.overlay, context.getTransition(), context);
    }
    private void draw(IEntity source, PoseStack stack, int light, int overlay, float delta, FormRenderingContext context) {
        var visual = visualPlayer(); if (visual == null || source == null) return;
        visual.sync(source, ReplayBindings.sample(source, delta), delta);
        // Editor/static previews can draw a form without ever calling Form.update().
        // Only a draw owns this native session update; matrix queries remain read-only.
        mcaxolotl.bbstacz.client.ActorAnimations.tick(source, visual);
        PlayerRenderer renderer = renderer(); var rig = getRig(); var poses = evaluated(source, delta);
        var consumers = FormUtilsClient.getProvider();
        boolean picking = context != null && context.isPicking();
        if (picking) {
            CustomVertexConsumerProvider.hijackVertexFormat(layer -> {
                setupTarget(context, BBSShaders.getPickerModelsProgram()); RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
            });
            consumers.setSubstitute(MobPickerVertexConsumer::new);
        }
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
        try (var state = new PlayerModelRenderState(renderer.getModel()); var guns = new mcaxolotl.bbstacz.client.NativeGunModels(visual)) {
            collectLocal(source, delta);
            try (var scope = new NativeRenderScope(form, source, visual, delta, poses, context != null && context.isPicking())) {
                MobRenderContext mob = MobRenderContext.push(rig, authoredPose(), null).picking(context == null ? null : context.stencilMap);
                try {
                    visual.hurtTime = overlay == 0 || (overlay >> 16 & 0xffff) == 10 ? 0 : 100;
                    Minecraft.getInstance().getEntityRenderDispatcher().render(visual, 0, 0, 0, 0, delta, stack, consumers, light);
                } finally { mob.pop(); }
                consumers.endBatch();
            }
        } finally {
            if (picking) { consumers.setSubstitute(null); CustomVertexConsumerProvider.clearRunnables(); }
            RenderSystem.getModelViewMatrix().set(modelView);
        }
    }
    private void collectLocal(IEntity source, float delta) {
        var visual = visualPlayer(); if (visual == null) return;
        visual.sync(source, ReplayBindings.sample(source, delta), delta); bones.clear();
        try (var state = new PlayerModelRenderState(renderer().getModel()); var scope = new NativeRenderScope(form, source, visual, delta, evaluated(source, delta), true)) {
            MobRigMatrices.evaluate(visual, getRig(), authoredPose(), null, delta, bones);
            mcaxolotl.bbstacz.client.NativeBends.matrices(renderer(), visual, getRig(), scope.pose, delta, bones);
            var offset = renderer().getRenderOffset(visual, delta);
            if (offset.lengthSqr() != 0) {
                var translation = new Matrix4f().translation((float)offset.x, (float)offset.y, (float)offset.z);
                for (var entry : bones.entrySet()) bones.put(entry.getKey(), new Matrix4f(translation).mul(entry.getValue().matrix()), new Matrix4f(translation).mul(entry.getValue().origin()));
            }
        }
    }
    @Override public void collectMatrices(IEntity source, PoseStack stack, MatrixCache cache, String prefix, float delta) {
        if (visualPlayer() == null) return;
        stack.pushPose(); applyTransforms(stack, true, delta); Matrix4f origin = new Matrix4f(stack.last().pose()); stack.popPose();
        stack.pushPose(); applyTransforms(stack, false, delta); cache.put(prefix, new Matrix4f(stack.last().pose()), origin);
        collectLocal(source, delta);
        for (var entry : bones.entrySet()) cache.put(StringUtils.combinePaths(prefix, entry.getKey()),
            new Matrix4f(stack.last().pose()).mul(entry.getValue().matrix()), new Matrix4f(stack.last().pose()).mul(entry.getValue().origin()));
        for (BodyPart part : form.parts.getAllTyped()) {
            if (part.getForm() == null) continue;
            stack.pushPose(); var matrix = part.filterBoneMatrix(bones.get(part.bone.get()).matrix()); if (matrix != null) MatrixStackUtils.multiply(stack, matrix);
            MatrixStackUtils.applyTransform(stack, part.transform.get());
            FormUtilsClient.getRenderer(part.getForm()).collectMatrices(source, stack, cache, StringUtils.combinePaths(prefix, part.getId()), delta); stack.popPose();
        }
        stack.popPose();
    }
    @Override public void renderBodyParts(FormRenderingContext context) {
        for (BodyPart part : form.parts.getAllTyped()) {
            var matrix = part.filterBoneMatrix(bones.get(part.bone.get()).matrix());
            if (matrix == null) { renderBodyPart(part, context); continue; }
            context.stack.pushPose(); if (context.world != null) context.world.pushPose();
            MatrixStackUtils.multiply(context.stack, matrix); if (context.world != null) MatrixStackUtils.multiply(context.world, matrix);
            renderBodyPart(part, context); context.stack.popPose(); if (context.world != null) context.world.popPose();
        }
    }
    @Override public boolean renderArm(PoseStack stack, int light, AbstractClientPlayer source, InteractionHand hand) {
        var visual = visualPlayer(); if (visual == null) return false;
        var entity = new MCEntity(source); visual.sync(entity, null); var consumers = FormUtilsClient.getProvider();
        try (var state = new PlayerModelRenderState(renderer().getModel()); var drawing = AnimationRenderContext.hand(visual)) {
            if (hand == InteractionHand.MAIN_HAND) renderer().renderRightHand(stack, consumers, light, visual); else renderer().renderLeftHand(stack, consumers, light, visual);
            consumers.endBatch();
        }
        return true;
    }
    @Override protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2) {
        if (visualPlayer() == null) return;
        IEntity source = actor != null ? actor : new StubEntity(Minecraft.getInstance().level);
        PoseStack stack = context.batcher.getContext().pose(); stack.pushPose();
        float scale = (y2 - y1) / 2.5f * form.uiScale.get();
        Matrix4f matrix = new Matrix4f().translate((x1 + x2) / 2f, y1 + (y2 - y1) * 0.85f, 40).scale(scale, -scale, scale)
            .rotateX((float) Math.PI / 8).rotateY((float) Math.PI + (context.mouseX - (x1 + x2) / 2f) * (float) Math.PI / 180);
        MatrixStackUtils.multiply(stack, matrix); applyTransforms(stack, false, context.getTransition());
        draw(source, stack, 15728880, 0, context.getTransition(), null); stack.popPose(); RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_ALWAYS);
    }
}
