package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.client.NativeRenderScope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerRenderer.class, priority = 500)
public abstract class NativeBodyRotationMixin {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void bbstacz$body(AbstractClientPlayer player, PoseStack pose, float age, float yaw, float delta, CallbackInfo ci) {
        var scope = NativeRenderScope.current();
        if (scope == null || scope.player != player) return;
        var body = scope.pose.get("anchor"); if (body == null) return;
        pose.translate(body.at(6), body.at(7) + 0.7, body.at(8));
        pose.mulPose(Axis.ZP.rotation(body.at(5))); pose.mulPose(Axis.YP.rotation(body.at(4))); pose.mulPose(Axis.XP.rotation(body.at(3)));
        pose.translate(0, -0.7, 0);
    }
}
