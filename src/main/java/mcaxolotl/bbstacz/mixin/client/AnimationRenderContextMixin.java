package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.client.animation.third.AnimationRenderContext;
import mcaxolotl.bbstacz.client.ThirdPersonPose;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AnimationRenderContext.class, remap = false)
public abstract class AnimationRenderContextMixin {
    @Inject(method = "isFirstPersonHand", at = @At("HEAD"), cancellable = true)
    private static void bbstacz$scratch(Object entity, CallbackInfoReturnable<Boolean> cir) {
        if (ThirdPersonPose.isSampling()) cir.setReturnValue(false);
    }
}
