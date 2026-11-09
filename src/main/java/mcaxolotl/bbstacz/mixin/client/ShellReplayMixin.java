package mcaxolotl.bbstacz.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.model.functional.ShellRender;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A recorded hand pass draws all origins/retained guns once, never through the current gun's shared queue. */
@Mixin(value = ShellRender.class, remap = false)
public abstract class ShellReplayMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/item/ItemDisplayContext;IILnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("HEAD"), cancellable = true)
    private void bbstacz$shells(PoseStack pose, VertexConsumer vertices, ItemDisplayContext mode, int light, int overlay, MultiBufferSource buffers, CallbackInfo ci) {
        if (FirstPersonPlayback.frame() != null && mode.firstPerson()) ci.cancel();
    }
}
