package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.client.particle.MuzzleParticleManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.world.InteractionHand;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Scope the native muzzle binder's mutable maps without exposing its private record types. */
@Mixin(value = MuzzleParticleManager.class, remap = false)
public interface MuzzleCaptureAccess {
    @Accessor("PENDING") static EnumMap<InteractionHand, List<Object>> pending() { throw new AssertionError(); }
    @Accessor("BOUND") static EnumMap<InteractionHand, List<Object>> bound() { throw new AssertionError(); }
    @Accessor("BASE_POSE") static EnumMap<InteractionHand, Matrix4f> bases() { throw new AssertionError(); }
    @Accessor("lightDisplay") static GunDisplayInstance lightDisplay() { throw new AssertionError(); }
    @Accessor("lightDisplay") static void lightDisplay(GunDisplayInstance value) { throw new AssertionError(); }
    @Accessor("lightShootTime") static long lightTime() { throw new AssertionError(); }
    @Accessor("lightShootTime") static void lightTime(long value) { throw new AssertionError(); }
    @Accessor("flashLightLevel") static int lightLevel() { throw new AssertionError(); }
    @Accessor("flashLightLevel") static void lightLevel(int value) { throw new AssertionError(); }
}
