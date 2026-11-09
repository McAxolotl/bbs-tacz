package mcaxolotl.bbstacz.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A recorded FP pass must not depend on the live player's current inventory or initialize a live Lua machine. */
@Mixin(value = ItemInHandRenderer.class, priority = 1100)
public abstract class RecordedHandPassMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void bbstacz$recorded(float delta, PoseStack pose, MultiBufferSource.BufferSource buffers, LocalPlayer player, int light, CallbackInfo ci) {
        var target = FirstPersonPlayback.target(delta); if (target == null) return;
        pose.pushPose();
        try {
            float[] q = target.frame().handCamera(); pose.mulPose(new Quaternionf(q[0], q[1], q[2], q[3]));
            if (FirstPersonPlayback.render(pose, buffers, light, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, delta)) ci.cancel();
        } finally { pose.popPose(); }
    }
}
