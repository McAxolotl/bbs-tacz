package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An owned first-person film plays recorded shots, never a held mouse button or a late live prediction too. */
@Mixin(value=LocalPlayerShoot.class,remap=false)
public abstract class RecordedGunInputMixin {
    @Inject(method="shoot",at=@At("HEAD"),cancellable=true)
    private void bbstacz$ownedInput(CallbackInfoReturnable<ShootResult> ci) { if(FirstPersonPlayback.ownsPlayer()) ci.setReturnValue(ShootResult.COOL_DOWN); }
    @Inject(method="lambda$doShoot$2",at=@At("HEAD"),cancellable=true)
    private void bbstacz$latePrediction(long epoch,ItemStack stack,GunDisplayInstance display,GunData data,CallbackInfo ci) { if(FirstPersonPlayback.ownsPlayer()) ci.cancel(); }
}
