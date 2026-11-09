package mcaxolotl.bbstacz.smoke.mixin;

import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.util.time.GunTime;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe the native server's decision; a client SUCCESS alone is not proof that a real bullet spawned. */
@Mixin(value=LivingEntityShoot.class,remap=false)
public abstract class ServerShotProbeMixin {
    @Shadow private LivingEntity shooter;
    @Shadow private ShooterDataHolder data;
    @Inject(method="shoot(Ljava/util/function/Supplier;Ljava/util/function/Supplier;JFZ)Lcom/tacz/guns/api/entity/ShootResult;",at=@At("RETURN"))
    private void bbstacz$result(Supplier<Float> pitch,Supplier<Float> yaw,long timestamp,float charge,boolean hasCharge,CallbackInfoReturnable<ShootResult> ci) {
        if(shooter instanceof ServerPlayer && ci.getReturnValue()==ShootResult.SUCCESS) mcaxolotl.bbstacz.smoke.ReviewProbe.serverSuccessfulShots++;
        if(shooter instanceof ServerPlayer) System.out.println("[REVIEW native server shot] result="+ci.getReturnValue()+" gun="+(data.currentGunItem==null?"null":data.currentGunItem.get())+" clock="+GunTime.nowMillis()+" base="+data.baseTimestamp+" packet="+timestamp+" alpha="+(GunTime.nowMillis()-data.baseTimestamp-timestamp));
    }
}
