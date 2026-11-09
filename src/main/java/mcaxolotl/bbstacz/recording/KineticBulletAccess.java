package mcaxolotl.bbstacz.recording;

import mchorse.bbs_mod.data.types.MapType;
import net.minecraft.world.phys.Vec3;

public interface KineticBulletAccess {
    MapType bbstacz$captureBallistics();
    void bbstacz$restoreBallistics(MapType state, Vec3 start);
}
