package mcaxolotl.bbstacz.smoke.mixin;

import mcaxolotl.bbstacz.smoke.ActionLifecycleProbe;
import mchorse.bbs_mod.actions.ActionPlayer;
import mchorse.bbs_mod.film.Film;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ActionPlayer.class,remap=false)
public abstract class ActionLifecycleProbeMixin {
    @Shadow public Film film;
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void bbstacz$disconnectWhileTicking(CallbackInfoReturnable<Boolean> ci) {
        if(film.getId().equals(ActionLifecycleProbe.film)&&ActionLifecycleProbe.tick!=null) {
            Runnable next=ActionLifecycleProbe.tick; ActionLifecycleProbe.tick=null; next.run(); ci.setReturnValue(true);
        }
    }
    @Inject(method="stop",at=@At("HEAD"))
    private void bbstacz$startAnotherWhileStopping(CallbackInfo ci) {
        if(film.getId().equals(ActionLifecycleProbe.film)&&ActionLifecycleProbe.stop!=null) {
            Runnable next=ActionLifecycleProbe.stop; ActionLifecycleProbe.stop=null; next.run();
        }
    }
}
