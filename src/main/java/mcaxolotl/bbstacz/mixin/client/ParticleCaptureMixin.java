package mcaxolotl.bbstacz.mixin.client;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.ParticleRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import mcaxolotl.bbstacz.client.NativeParticles;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ParticleRenderer.class, remap = false)
public abstract class ParticleCaptureMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private static void bbstacz$capture(ParticleEmitterInstance emitter, PoseStack pose, MultiBufferSource buffers,
                                      int light, float delta, float pitch, float roll, Matrix4f camera, CallbackInfo ci) {
        NativeParticles.capture(emitter, pose, pitch, roll, camera);
    }
}
