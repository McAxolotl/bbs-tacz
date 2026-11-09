package mcaxolotl.bbstacz.smoke.mixin;

import mcaxolotl.bbstacz.client.NativeRenderScope;
import mcaxolotl.bbstacz.smoke.ReviewProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class ReviewPoseProbeMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V", at = @At("HEAD"))
    private void bbstacz$draw(com.mojang.blaze3d.vertex.PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer vertices,
        int light, int overlay, float r, float g, float b, float a, CallbackInfo ci) {
        var scope = NativeRenderScope.current();
        if (scope == null || scope.matrices) return;
        var model = ((PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(scope.player)).getModel();
        if ((Object)this == model.rightArm) ReviewProbe.player(model);
    }
}
