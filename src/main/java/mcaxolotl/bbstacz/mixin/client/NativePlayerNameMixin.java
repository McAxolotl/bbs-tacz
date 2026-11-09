package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.client.forms.TaczVisualPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** BBS draws the actor's authored name; a render-only proxy must not add a fictitious player nameplate. */
@Mixin(LivingEntityRenderer.class)
public abstract class NativePlayerNameMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void bbstacz$proxy(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) { if (entity instanceof TaczVisualPlayer) cir.setReturnValue(false); }
}
