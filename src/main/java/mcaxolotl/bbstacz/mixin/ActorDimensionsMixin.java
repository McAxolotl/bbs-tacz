package mcaxolotl.bbstacz.mixin;

import mcaxolotl.bbstacz.recording.PhysicalCrawl;
import mchorse.bbs_mod.entity.ActorEntity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ActorEntity.class)
public abstract class ActorDimensionsMixin {
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void bbstacz$dimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (pose == Pose.SWIMMING && PhysicalCrawl.isCrawling((LivingEntity) (Object) this)) {
            EntityDimensions d = cir.getReturnValue(); cir.setReturnValue(EntityDimensions.scalable(d.width, Math.min(d.height, 0.6f)));
        }
    }
    @Inject(method = "getStandingEyeHeight", at = @At("RETURN"), cancellable = true)
    private void bbstacz$eye(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if (PhysicalCrawl.isCrawling((LivingEntity) (Object) this)) cir.setReturnValue(Math.min(cir.getReturnValue(), 0.4f));
    }
}
