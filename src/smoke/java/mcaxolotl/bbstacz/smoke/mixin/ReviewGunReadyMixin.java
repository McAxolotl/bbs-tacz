package mcaxolotl.bbstacz.smoke.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.BedrockGunModel;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import mcaxolotl.bbstacz.smoke.ReviewProbe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A native draw observation keeps startup/resource reloads out of the recording fixture. */
@Mixin(value = BedrockGunModel.class, remap = false)
public abstract class ReviewGunReadyMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IIFFFFLnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("TAIL"))
    private void bbstacz$draw(PoseStack stack, ItemStack gun, ItemDisplayContext mode, RenderType type, int light, int overlay, float r, float g, float b, float a, MultiBufferSource buffers, CallbackInfo ci) {
        if(mode.firstPerson() && !FirstPersonPlayback.isRendering()) ReviewProbe.liveGunDraws++;
    }
}
