package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.client.particle.MuzzleParticleManager;
import mcaxolotl.bbstacz.client.MirrorParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** F5 mirror transforms use a virtual FP eye, including front-facing F5. No live camera mutation. */
@Mixin(value = MuzzleParticleManager.class, remap = false)
public abstract class MirrorMuzzleCameraMixin {
    @Redirect(method = "spawnAndBind", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getMainCamera()Lnet/minecraft/client/Camera;", remap = true))
    private static Camera bbstacz$eye(GameRenderer renderer) { return MirrorParticles.active() ? MirrorParticles.camera() : renderer.getMainCamera(); }
}
