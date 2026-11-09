package mcaxolotl.bbstacz.mixin.client;

import cn.sh1rocu.tacz.mixin.common.accessor.BlockableEventLoopAccessor;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import mcaxolotl.bbstacz.client.ClockedGunTasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value=LocalPlayerShoot.class,remap=false)
public abstract class ClockedGunSchedulingMixin {
    @Redirect(method="doShoot",at=@At(value="INVOKE",target="Ljava/util/concurrent/ScheduledExecutorService;scheduleAtFixedRate(Ljava/lang/Runnable;JJLjava/util/concurrent/TimeUnit;)Ljava/util/concurrent/ScheduledFuture;"))
    private ScheduledFuture<?> bbstacz$clockedBurst(ScheduledExecutorService executor,Runnable action,long delay,long period,TimeUnit unit) {
        return ClockedGunTasks.schedule(action,delay,period,unit);
    }
    @Redirect(method="lambda$doShoot$3",at=@At(value="INVOKE",target="Lcn/sh1rocu/tacz/mixin/common/accessor/BlockableEventLoopAccessor;tacz$submitAsync(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Void> bbstacz$sameGunInstant(BlockableEventLoopAccessor client,Runnable effect) {
        // We already run on the client thread at the scheduled logical instant. Enqueuing
        // again would shift flash/sound/animation to a later captured frame after a slow render.
        if(ClockedGunTasks.running()) { effect.run(); return CompletableFuture.completedFuture(null); }
        return client.tacz$submitAsync(effect);
    }
}
