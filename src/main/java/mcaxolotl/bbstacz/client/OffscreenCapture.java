package mcaxolotl.bbstacz.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.event.FirstPersonRenderGunEvent;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import java.util.List;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.PhysicalCrawl;
import mcaxolotl.bbstacz.recording.ShellSnapshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/** Keeps the FP gun pose recordable while the player films in F5. Does not draw or open the refit GUI. */
public final class OffscreenCapture {
    private OffscreenCapture() {}
    public static FrameSnapshot evaluate(LocalPlayer player, ItemStack stack, List<ShellSnapshot> shells, long shot, float age, float flashRoll) {
        var display = TimelessAPI.getGunDisplay(stack).orElse(null);
        if (display == null || display.getGunModel() == null) return null;
        var model = display.getGunModel(); var state = display.getAnimationStateMachine();
        var saved = NativePose.capture(model); var savedCamera = new Quaternionf(model.getCameraAnimationObject().rotationQuaternion);
        try {
            if (state != null) {
                var renderer = GunItemRendererWrapper.INSTANCE.get();
                if (!state.isInitialized()) renderer.tryInit(stack, player, 0);
                state.processContextIfExist(context -> renderer.updateContext(context, stack, player, 0));
                // TACZ's visualUpdate already advances the Lua machine in third person. Only evaluate its runners here.
                state.getAnimationController().update();
            }
            PoseStack pose = new PoseStack();
            float xRot = player.getViewXRot(0) - player.xBobO, yRot = player.getViewYRot(0) - player.yBobO;
            pose.mulPose(Axis.XP.rotationDegrees(xRot * -0.1f)); pose.mulPose(Axis.YP.rotationDegrees(yRot * -0.1f));
            var root = model.getRootNode();
            if (root != null) {
                xRot = (float) Math.tanh(xRot / 25) * 25; yRot = (float) Math.tanh(yRot / 25) * 25;
                root.offsetX += yRot * 0.1f / 48; root.offsetY -= xRot * 0.1f / 48;
                root.additionalQuaternion.mul(Axis.XP.rotationDegrees(xRot * 0.05f)).mul(Axis.YP.rotationDegrees(yRot * 0.05f));
            }
            pose.translate(0, 1.5, 0); pose.mulPose(Axis.ZP.rotationDegrees(180));
            FirstPersonRenderGunEvent.applyFirstPersonGunTransform(player, stack, pose, model, 0);
            CapturedShells.evaluated(model, pose);
            Quaternionf hand = new Quaternionf(model.getCameraAnimationObject().rotationQuaternion);
            var gun = com.tacz.guns.api.item.IGun.getIGunOrNull(stack);
            if (gun != null) {
                float aim = com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(0);
                float multiplier = 1 - aim + aim / (float) Math.sqrt(gun.getAimingZoom(stack));
                hand = com.tacz.guns.util.math.MathUtil.multiplyQuaternion(hand, multiplier);
            }
            return new FrameSnapshot(stack.save(new CompoundTag()).toString(), true, NativePose.capture(model), ThirdPersonPose.sample(player, 0),
                pose.last().pose().get(new float[16]), RefitTransform.getOpeningProgress() == 0 && model.getRenderHand(), PhysicalCrawl.isCrawling(player),
                IGunOperator.fromLivingEntity(player).getSynAimingProgress(), RefitTransform.getOpeningProgress(), shot, age, flashRoll,
                CapturedShells.snapshot(null), new float[]{hand.x, hand.y, hand.z, hand.w},
                MirrorParticles.evaluate(model, pose, state == null ? List.of() : state.getAnimationController().consumePendingParticles()));
        } finally {
            NativePose.apply(model, saved); model.getCameraAnimationObject().rotationQuaternion.set(savedCamera);
        }
    }
}
