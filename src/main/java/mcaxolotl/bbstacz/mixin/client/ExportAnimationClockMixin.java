package mcaxolotl.bbstacz.mixin.client;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.PausedClientAnimationClock;
import mcaxolotl.bbstacz.client.ExportGunClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=PausedClientAnimationClock.class, remap=false)
public abstract class ExportAnimationClockMixin {
    @Inject(method="nowNanos", at=@At("RETURN"), cancellable=true)
    private void bbstacz$videoTime(CallbackInfoReturnable<Long> ci) { ci.setReturnValue(ExportGunClock.sample(ci.getReturnValue())); }
    @Inject(method="reset", at=@At("TAIL"))
    private void bbstacz$reset(CallbackInfo ci) { ExportGunClock.reset(); }
}
