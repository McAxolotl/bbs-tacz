package mcaxolotl.bbstacz.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = GunItemRendererWrapper.class, remap = false)
public interface GunMuzzleAccess {
    @Invoker("cacheMuzzlePosition")
    static void bbstacz$cache(PoseStack pose, BedrockGunModel model) { throw new AssertionError(); }
}
