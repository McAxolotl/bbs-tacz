package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MuzzleFlashRender.class, remap = false)
public interface MuzzleFlashAccess {
    @Accessor("TP_SHOOT_TIMESTAMPS") static java.util.Map<net.minecraft.world.entity.LivingEntity, Long> bbstacz$thirdPersonTimes() { throw new AssertionError(); }
    @Accessor("shootTimeStamp") static long bbstacz$time() { throw new AssertionError(); }
    @Accessor("shootTimeStamp") static void bbstacz$time(long time) { throw new AssertionError(); }
    @Accessor("muzzleFlashRandomRotate") static float bbstacz$rotation() { throw new AssertionError(); }
    @Accessor("muzzleFlashRandomRotate") static void bbstacz$rotation(float rotation) { throw new AssertionError(); }
}
