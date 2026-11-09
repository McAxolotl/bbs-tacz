package mcaxolotl.bbstacz.smoke.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import mcaxolotl.bbstacz.client.forms.TaczVisualPlayer;
import mcaxolotl.bbstacz.smoke.MountProbe;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class HandNeutralProbeMixin {
    @Inject(method = "renderHand", at = @At("TAIL"))
    private void bbstacz$neutral(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        if (FirstPersonPlayback.isRendering() && player instanceof TaczVisualPlayer && (Math.abs(arm.xRot)+Math.abs(arm.yRot)+Math.abs(arm.zRot) > 1e-5)) MountProbe.neutral = false;
    }
}
