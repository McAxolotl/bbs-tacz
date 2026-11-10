package mcaxolotl.bbstacz.smoke.mixin;

import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import mcaxolotl.bbstacz.smoke.ReviewProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe the real trigger result, without issuing a fallback shot or bypassing native state locks. */
@Mixin(value=LocalPlayerShoot.class,remap=false)
public abstract class NativeInputResultProbeMixin {
    @Inject(method="shoot",at=@At("RETURN"))
    private void bbstacz$result(CallbackInfoReturnable<ShootResult> ci) {
        if(ReviewProbe.shootResult!=null) ReviewProbe.shootResult.accept(ci.getReturnValue());
    }
}
