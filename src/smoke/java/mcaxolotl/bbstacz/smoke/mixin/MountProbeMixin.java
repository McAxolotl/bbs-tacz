package mcaxolotl.bbstacz.smoke.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import mcaxolotl.bbstacz.smoke.MountProbe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BedrockGunModel.class, remap = false)
public abstract class MountProbeMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IIFFFFLnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("TAIL"))
    private void bbstacz$probe(PoseStack pose, ItemStack gun, ItemDisplayContext mode, RenderType type, int light, int overlay, float r, float g, float b, float a, MultiBufferSource buffers, CallbackInfo ci) {
        if (!MountProbe.active || mode != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) return;
        var part = ((BedrockGunModel) (Object) this).getNode("shell"); if (part == null) return;
        var path = new java.util.ArrayList<BedrockPart>(); for (var p = part; p != null; p = p.getParent()) path.add(0,p);
        pose.pushPose(); try { path.forEach(p -> p.translateAndRotateAndScale(pose)); MountProbe.matrix = new org.joml.Matrix4f(pose.last().pose()); } finally { pose.popPose(); }
    }
}
