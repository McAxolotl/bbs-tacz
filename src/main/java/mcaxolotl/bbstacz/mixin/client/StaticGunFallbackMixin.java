package mcaxolotl.bbstacz.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.item.ExternalGunItemVboRenderer;
import com.tacz.guns.client.renderer.item.StaticGunRenderResult;
import mcaxolotl.bbstacz.client.NativeRenderScope;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** BBS needs functional shell/muzzle nodes, which the static VBO handoff deliberately does not evaluate. */
@Mixin(value = ExternalGunItemVboRenderer.class, remap = false)
public abstract class StaticGunFallbackMixin {
    @Inject(method = "tryRender", at = @At("HEAD"), cancellable = true)
    private static void bbstacz$functional(ItemStack stack, ItemDisplayContext mode, boolean lod, PoseStack pose,
                                          MultiBufferSource buffers, int light, int overlay, CallbackInfoReturnable<StaticGunRenderResult> cir) {
        if (NativeRenderScope.current() != null && (mode == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || mode == ItemDisplayContext.FIXED)) cir.setReturnValue(StaticGunRenderResult.UNSUPPORTED);
    }
}
