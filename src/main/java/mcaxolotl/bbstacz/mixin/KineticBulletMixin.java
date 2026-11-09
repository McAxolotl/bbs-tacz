package mcaxolotl.bbstacz.mixin;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage.DistanceDamagePair;
import java.util.LinkedList;
import mcaxolotl.bbstacz.recording.KineticBulletAccess;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Spawn packets omit damage/fuse/block-destruction fields. Capture those explicitly, not by reflection. */
@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class KineticBulletMixin implements KineticBulletAccess {
    @Shadow private LinkedList<DistanceDamagePair> damageAmount;
    @Shadow private Vec3 startPos;
    @Shadow private float distanceAmount;
    @Shadow private float knockback;
    @Shadow private float explosionDamage;
    @Shadow private float explosionRadius;
    @Shadow private float damageModifier;
    @Shadow private float armorIgnore;
    @Shadow private float headShot;
    @Shadow private float shotDamageMultiplier;
    @Shadow private int igniteEntityTime;
    @Shadow private int explosionDelayCount;
    @Shadow private boolean explosionKnockback;
    @Shadow private boolean explosionDestroyBlock;
    @Override public MapType bbstacz$captureBallistics() {
        MapType map = new MapType();
        map.putFloat("distanceAmount", distanceAmount);
        map.putFloat("knockback", knockback);
        map.putFloat("explosionDamage", explosionDamage);
        map.putFloat("explosionRadius", explosionRadius);
        map.putFloat("damageModifier", damageModifier);
        map.putFloat("armorIgnore", armorIgnore);
        map.putFloat("headShot", headShot);
        map.putFloat("shotDamageMultiplier", shotDamageMultiplier);
        map.putInt("igniteEntityTime", igniteEntityTime);
        map.putInt("explosionDelayCount", explosionDelayCount);
        map.putBool("explosionKnockback", explosionKnockback);
        map.putBool("explosionDestroyBlock", explosionDestroyBlock);
        ListType damage = new ListType();
        for (DistanceDamagePair pair : damageAmount) {
            ListType p = new ListType(); p.addFloat(pair.getDistance()); p.addFloat(pair.getDamage()); damage.add(p);
        }
        map.put("damage", damage);
        return map;
    }
    @Override public void bbstacz$restoreBallistics(MapType map, Vec3 start) {
        startPos = start;
        distanceAmount = map.getFloat("distanceAmount");
        knockback = map.getFloat("knockback");
        explosionDamage = map.getFloat("explosionDamage");
        explosionRadius = map.getFloat("explosionRadius");
        damageModifier = map.getFloat("damageModifier");
        armorIgnore = map.getFloat("armorIgnore");
        headShot = map.getFloat("headShot");
        shotDamageMultiplier = map.getFloat("shotDamageMultiplier");
        igniteEntityTime = map.getInt("igniteEntityTime");
        explosionDelayCount = map.getInt("explosionDelayCount");
        explosionKnockback = map.getBool("explosionKnockback");
        explosionDestroyBlock = map.getBool("explosionDestroyBlock");
        damageAmount = new LinkedList<>();
        ListType damage = map.getList("damage");
        if (damage.size() > 4096) throw new IllegalArgumentException("Oversized damage curve");
        for (BaseType element : damage) {
            ListType p = element.asList();
            damageAmount.add(new DistanceDamagePair(p.getFloat(0), p.getFloat(1)));
        }
    }
}
