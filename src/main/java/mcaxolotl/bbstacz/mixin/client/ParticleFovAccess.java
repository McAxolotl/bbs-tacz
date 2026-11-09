package mcaxolotl.bbstacz.mixin.client;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.CameraStateCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Particle projection scaling must use the recorded FP/world FOV pair rather than the viewer's live item. */
@Mixin(value = CameraStateCache.class, remap = false)
public interface ParticleFovAccess {
    @Accessor("worldFov") static float world() { throw new AssertionError(); }
    @Accessor("worldFov") static void world(float value) { throw new AssertionError(); }
    @Accessor("handFov") static float hand() { throw new AssertionError(); }
    @Accessor("handFov") static void hand(float value) { throw new AssertionError(); }
}
