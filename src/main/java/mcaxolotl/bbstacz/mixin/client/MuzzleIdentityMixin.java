package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.client.particle.ThirdPersonMuzzleParticleManager;
import mcaxolotl.bbstacz.client.NativeRenderScope;
import mchorse.bbs_mod.forms.entities.MCEntity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Resolve visual-player muzzle anchors back to their actual actor without reposting gameplay events. */
@Mixin(value = ThirdPersonMuzzleParticleManager.class, remap = false)
public abstract class MuzzleIdentityMixin {
    @Inject(method = "onMuzzleRendered", at = @At("HEAD"), cancellable = true)
    private static void bbstacz$actor(LivingEntity entity, Matrix4f world, CallbackInfo ci) {
        var scope = NativeRenderScope.current();
        if (scope == null || scope.player != entity) return;
        if (!scope.matrices && scope.frame == null && scope.entity instanceof MCEntity mc && mc.getMcEntity() instanceof LivingEntity actor)
            ThirdPersonMuzzleParticleManager.onMuzzleRendered(actor, world);
        ci.cancel();
    }
}
