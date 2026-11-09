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

/** Runs after BBS's hitbox override, so an authored standing hitbox cannot prevent real crawling. */
@Mixin(value = Player.class, priority = 900)
public abstract class PlayerDimensionsMixin {
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void bbstacz$dimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (pose == Pose.SWIMMING && PhysicalCrawl.isCrawling((LivingEntity) (Object) this)) {
            var size = cir.getReturnValue(); cir.setReturnValue(EntityDimensions.scalable(size.width, Math.min(0.6f, size.height)));
        }
    }
}
