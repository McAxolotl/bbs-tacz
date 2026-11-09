package mcaxolotl.bbstacz.mixin;

import com.tacz.guns.entity.shooter.LivingEntityCrawl;
import mcaxolotl.bbstacz.recording.PhysicalCrawl;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A validated recorded pose is not a fresh key press. Native input validation would clear a gunless Actor each tick. */
@Mixin(value = LivingEntityCrawl.class, remap = false)
public abstract class RecordedCrawlValidationMixin {
    @Shadow @Final private LivingEntity shooter;
    @Inject(method = "tickCrawling", at = @At("HEAD"), cancellable = true)
    private void bbstacz$recorded(CallbackInfo ci) {
        if (PhysicalCrawl.isManaged(shooter)) ci.cancel();
    }
}
