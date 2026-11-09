package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.util.math.SecondOrderDynamics;
import com.tacz.guns.util.time.GunTime;
import java.util.concurrent.ScheduledExecutorService;
import mcaxolotl.bbstacz.recording.ClockedSpring;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No racing worker may mutate a spring while a fixed-time video frame is being rendered. */
@Mixin(value=SecondOrderDynamics.class, remap=false)
public abstract class ClockedGunDynamicsMixin {
    @Shadow private boolean stop;
    @Unique private ClockedSpring bbstacz$spring;
    @Redirect(method="<init>", at=@At(value="INVOKE", target="Ljava/util/concurrent/ScheduledExecutorService;execute(Ljava/lang/Runnable;)V"))
    private void bbstacz$noWallClockWorker(ScheduledExecutorService executor,Runnable worker) {}
    @Inject(method="<init>", at=@At("TAIL"))
    private void bbstacz$init(float f,float z,float r,float x,CallbackInfo ci) { bbstacz$spring=new ClockedSpring(f,z,r,x,GunTime.nowNanos()); }
    @Inject(method="update(F)F", at=@At("HEAD"), cancellable=true)
    private void bbstacz$target(float target,CallbackInfoReturnable<Float> ci) { ci.setReturnValue(bbstacz$spring.update(target,GunTime.nowNanos(),stop)); }
    @Inject(method="get", at=@At("HEAD"), cancellable=true)
    private void bbstacz$value(CallbackInfoReturnable<Float> ci) { ci.setReturnValue(bbstacz$spring.get(GunTime.nowNanos(),stop)); }
}
