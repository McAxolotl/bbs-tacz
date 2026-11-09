package mcaxolotl.bbstacz.actions;

import com.tacz.guns.entity.EntityKineticBullet;
import io.netty.buffer.Unpooled;
import mcaxolotl.bbstacz.recording.KineticBulletAccess;
import mcaxolotl.bbstacz.recording.ValuePayload;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** One recorded, successfully spawned pellet/rocket, not one trigger pull. Burst/shotgun counts stay exact. */
public final class TaczProjectileActionClip extends ActionClip {
    public final ValuePayload payload = new ValuePayload("bbs_tacz:projectile");
    public TaczProjectileActionClip() { add(payload); }
    public static TaczProjectileActionClip capture(EntityKineticBullet bullet, LivingEntity owner) {
        TaczProjectileActionClip clip = new TaczProjectileActionClip();
        MapType map = ((KineticBulletAccess) bullet).bbstacz$captureBallistics();
        FriendlyByteBuf bytes = new FriendlyByteBuf(Unpooled.buffer());
        try {
            bullet.writeSpawnData(bytes);
            byte[] spawn = new byte[bytes.readableBytes()]; bytes.getBytes(0, spawn); map.putByteArray("spawn", spawn);
        } finally { bytes.release(); }
        Vec3 offset = bullet.position().subtract(owner.position());
        map.putDouble("x", offset.x); map.putDouble("y", offset.y); map.putDouble("z", offset.z);
        map.putFloat("owner_yaw", owner.getYRot()); map.putFloat("owner_pitch", owner.getXRot());
        clip.payload.set(map);
        return clip;
    }
    @Override public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick) {
        if (actor == null) { applyPositionRotation(player, replay, tick); actor = player; }
        if (actor.level().isClientSide || payload.get().getByteArray("spawn").length == 0) return;
        MapType map = payload.get();
        byte[] data = map.getByteArray("spawn");
        if (data.length > 65_536) throw new IllegalArgumentException("Oversized TACZ projectile spawn");
        EntityKineticBullet bullet = new EntityKineticBullet(EntityKineticBullet.TYPE, actor.level());
        FriendlyByteBuf bytes = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        try { bullet.readSpawnData(bytes); } finally { bytes.release(); }
        // Do not keep the original recorded entity id / owner UUID.
        bullet.setOwner(actor);
        Quaternionf recorded = view(map.getFloat("owner_pitch"), map.getFloat("owner_yaw"));
        Quaternionf rotation = view(actor.getXRot(), actor.getYRot()).mul(recorded.invert());
        Vector3f offset = rotation.transform(new Vector3f((float) map.getDouble("x"), (float) map.getDouble("y"), (float) map.getDouble("z")));
        bullet.setPos(actor.getX() + offset.x, actor.getY() + offset.y, actor.getZ() + offset.z);
        Vector3f velocity = rotation.transform(bullet.getDeltaMovement().toVector3f());
        bullet.setDeltaMovement(velocity.x, velocity.y, velocity.z);
        bullet.setYRot(bullet.getYRot() + actor.getYRot() - map.getFloat("owner_yaw"));
        bullet.setXRot(bullet.getXRot() + actor.getXRot() - map.getFloat("owner_pitch"));
        ((KineticBulletAccess) bullet).bbstacz$restoreBallistics(map, bullet.position());
        // Native entity tick, collision, damage, penetration, ignition, fuse and explosion logic remain TACZ's.
        actor.level().addFreshEntity(bullet);
    }
    private static Quaternionf view(float pitch, float yaw) {
        return new Quaternionf().rotationY((float) Math.toRadians(-yaw)).rotateX((float) Math.toRadians(pitch));
    }
    @Override protected Clip create() { return new TaczProjectileActionClip(); }
}
