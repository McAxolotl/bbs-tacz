package mcaxolotl.bbstacz.mixin.client;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.firstperson.FirstPersonParticleSystem;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import mcaxolotl.bbstacz.client.MirrorParticles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FirstPersonParticleSystem.class, remap = false)
public abstract class MirrorWorldParticleMixin {
    @Redirect(method = "updateCameraVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getMainCamera()Lnet/minecraft/client/Camera;", remap = true))
    private net.minecraft.client.Camera bbstacz$eye(net.minecraft.client.renderer.GameRenderer renderer) {
        return MirrorParticles.active() ? MirrorParticles.camera() : renderer.getMainCamera();
    }
    @Inject(method = "deliverWorldSpaceParticle", at = @At("HEAD"), cancellable = true)
    private void bbstacz$isolated(ParticleInstance particle, ParticleEffectDefinition definition, ParticleEmitterInstance emitter, CallbackInfo ci) {
        if (MirrorParticles.active()) { MirrorParticles.interceptWorldParticle(particle, definition, emitter); ci.cancel(); }
    }
}
