package mcaxolotl.bbstacz.smoke.mixin;

import mcaxolotl.bbstacz.client.forms.TaczFormRenderer;
import mcaxolotl.bbstacz.smoke.MountProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TaczFormRenderer.class, remap = false)
public abstract class ArmProbeMixin {
    @Inject(method = "renderArm", at = @At("HEAD"))
    private void bbstacz$arm(com.mojang.blaze3d.vertex.PoseStack pose, int light, net.minecraft.client.player.AbstractClientPlayer player, net.minecraft.world.InteractionHand hand, CallbackInfoReturnable<Boolean> cir) { MountProbe.arms++; }
}
