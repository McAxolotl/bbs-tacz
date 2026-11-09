package mcaxolotl.bbstacz.mixin;

import mcaxolotl.bbstacz.recording.PhysicalCrawl;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Takes precedence over BBS's standing eye-height HEAD override. */
@Mixin(value = Player.class, priority = 2000)
public abstract class PlayerEyeMixin {
    @Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
    private void bbstacz$eye(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if (pose == Pose.SWIMMING && PhysicalCrawl.isCrawling((LivingEntity) (Object) this)) cir.setReturnValue(0.4f);
    }
}
