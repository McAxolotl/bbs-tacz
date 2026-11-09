package mcaxolotl.bbstacz.mixin;

import mcaxolotl.bbstacz.actions.TaczFireActionClip;
import mcaxolotl.bbstacz.actions.TaczProjectileActionClip;
import mchorse.bbs_mod.film.Film;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Old/staggered action takes must not lose a real shot solely because it outlives the last pose key. */
@Mixin(value=Film.class,remap=false)
public abstract class TaczFilmDurationMixin {
    @Inject(method="calculateDuration",at=@At("RETURN"),cancellable=true)
    private void bbstacz$actionTail(CallbackInfoReturnable<Integer> ci) {
        int end=ci.getReturnValue();
        for(var replay:((Film)(Object)this).replays.getList()) if(replay.enabled.get()) for(var clip:replay.actions.getClips(mchorse.bbs_mod.actions.types.ActionClip.class))
            if(clip.enabled.get() && (clip instanceof TaczProjectileActionClip || clip instanceof TaczFireActionClip))
                end=(int)Math.min(Integer.MAX_VALUE,Math.max((long)end,(long)clip.tick.get()+Math.max(1,clip.duration.get())));
        ci.setReturnValue(end);
    }
}
