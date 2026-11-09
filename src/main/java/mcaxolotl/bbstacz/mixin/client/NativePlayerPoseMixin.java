package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.client.NativeRenderScope;
import mcaxolotl.bbstacz.client.NativePlayerPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** After TACZ and Player Animator evaluate, before BBS's authored pose is added by its renderer hook. */
@Mixin(value = PlayerModel.class, priority = 500)
public abstract class NativePlayerPoseMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void bbstacz$pose(LivingEntity entity, float limb, float speed, float age, float yaw, float pitch, CallbackInfo ci) {
        var scope = NativeRenderScope.current();
        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        if (scope != null && scope.player == entity) NativePlayerPose.apply(model, scope.pose);
        else if (entity instanceof mcaxolotl.bbstacz.client.forms.TaczVisualPlayer
            && mcaxolotl.bbstacz.client.FirstPersonPlayback.isRendering()
            && com.tacz.guns.client.animation.third.AnimationRenderContext.isFirstPersonHand(entity)) {
            // Native TACZ normally checks the live kept item. A film gun can be rendered over an empty viewer hand.
            model.rightArm.xRot = model.rightArm.yRot = model.rightArm.zRot = 0;
            model.leftArm.xRot = model.leftArm.yRot = model.leftArm.zRot = 0;
            model.rightSleeve.copyFrom(model.rightArm); model.leftSleeve.copyFrom(model.leftArm);
        }
    }
}
