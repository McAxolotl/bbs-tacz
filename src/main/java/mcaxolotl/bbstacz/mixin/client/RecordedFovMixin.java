package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import mcaxolotl.bbstacz.client.LiveCapture;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Run after native viewport/BBS FOV modifiers: record the actual final value, or make the saved value authoritative. */
@Mixin(value = GameRenderer.class, priority = 500)
public abstract class RecordedFovMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void bbstacz$fov(Camera camera, float delta, boolean configured, CallbackInfoReturnable<Double> cir) {
        var target = FirstPersonPlayback.target(delta);
        if (target != null) {
            float fov = configured ? target.frame().worldFov() : target.frame().modelFov();
            if (fov > 0) cir.setReturnValue((double) fov);
        } else LiveCapture.fov(configured, cir.getReturnValue());
    }
}
