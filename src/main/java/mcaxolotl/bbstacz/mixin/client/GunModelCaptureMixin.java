package mcaxolotl.bbstacz.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.BedrockGunModel;
import mcaxolotl.bbstacz.client.LiveCapture;
import mcaxolotl.bbstacz.client.ThirdPersonShells;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BedrockGunModel.class, remap = false)
public abstract class GunModelCaptureMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IIFFFFLnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("TAIL"))
    private void bbstacz$pose(PoseStack stack, ItemStack gun, ItemDisplayContext mode, RenderType type,
                             int light, int overlay, float r, float g, float b, float a, MultiBufferSource buffers, CallbackInfo ci) {
        if (mode.firstPerson()) LiveCapture.captureModel((BedrockGunModel) (Object) this, stack, gun, light);
        else if (mode == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) ThirdPersonShells.capture((BedrockGunModel) (Object) this, stack, gun);
    }
}
