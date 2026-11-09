package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import mcaxolotl.bbstacz.actions.TaczProjectileActionClip;
import mcaxolotl.bbstacz.recording.KineticBulletAccess;
import mcaxolotl.bbstacz.recording.PhysicalCrawl;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.entity.ActorEntity;
import mcaxolotl.bbstacz.forms.TaczForm;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;

/** Runs on the integrated SERVER thread, in a newly created isolated development world. */
final class WorldSmoke {
    static int run(MinecraftServer server) {
        int count = 0;
        var level = server.overworld(); var owner = server.getPlayerList().getPlayers().get(0);
        var index = TimelessAPI.getAllCommonGunIndex().stream().findFirst().orElseThrow();
        var id = index.getKey(); var data = index.getValue();
        var gun = GunItemBuilder.create().setId(id).setAmmoCount(100).setAmmoInBarrel(true).setFireMode(FireMode.SEMI).build();
        require(!gun.isEmpty(), "Native gun builder failed"); count++;
        owner.setItemSlot(EquipmentSlot.MAINHAND, gun);
        IGunOperator.fromLivingEntity(owner).getDataHolder().setCurrentGunItem(owner::getMainHandItem);
        AttachmentPropertyManager.postChangeEvent(owner, gun);
        require(IGunOperator.fromLivingEntity(owner).getCacheProperty() != null, "Native gun properties did not initialize"); count++;
        var source = new EntityKineticBullet(level, owner, gun, data.getGunData().getAmmoId(), id, true, data.getGunData(), data.getBulletData());
        source.setDeltaMovement(0.01, 0.02, 3); source.setShotDamageMultiplier(2);
        require(source instanceof KineticBulletAccess, "Ballistic mixin did not apply"); count++;
        var clip = TaczProjectileActionClip.capture(source, owner);
        TaczProjectileActionClip copy = new TaczProjectileActionClip(); copy.fromData(clip.toData());
        ActorEntity actor = new ActorEntity(BBSMod.ACTOR_ENTITY, level); actor.setPos(owner.getX(), owner.getY(), owner.getZ());
        actor.setYRot(owner.getYRot()); actor.setXRot(owner.getXRot());
        level.addFreshEntity(actor);
        copy.applyAction(actor, null, null, null, 0);
        EntityKineticBullet replayed = null;
        for (var entity : level.getAllEntities()) if (entity instanceof EntityKineticBullet bullet && bullet.getOwner() == actor) replayed = bullet;
        require(replayed != null, "Playback did not spawn a real server-side projectile");
        mcaxolotl.bbstacz.recording.ReplayProjectiles.clearAll();
        require(!replayed.isRemoved(), "Film cleanup removed an unbound native projectile"); count++;
        require(replayed.getDeltaMovement().distanceTo(source.getDeltaMovement()) < 1e-5, "Projectile velocity changed"); count++;
        require(((KineticBulletAccess) source).bbstacz$captureBallistics().equals(((KineticBulletAccess) replayed).bbstacz$captureBallistics()), "Damage/fuse/penetration state changed"); count++;
        TaczForm form = new TaczForm(); form.hitbox.set(true); form.hitboxHeight.set(3f); form.hitboxEyeHeight.set(2f);
        actor.setForm(form);
        PhysicalCrawl.apply(actor, true);
        require(actor.getPose() == Pose.SWIMMING && !actor.isSwimming(), "Dry crawl was mistaken for water swimming"); count++;
        require(actor.getDimensions(Pose.SWIMMING).height <= 0.6f, "Authored standing hitbox prevented crawling"); count++;
        require(actor.getEyeHeight() <= 0.4f, "Crawling eye height is incorrect"); count++;
        actor.tick();
        require(actor.getPose() == Pose.SWIMMING && PhysicalCrawl.isCrawling(actor), "Native crawl validation cleared a recorded Actor mid-tick"); count++;
        PhysicalCrawl.apply(actor, false); PhysicalCrawl.release(actor);
        require(actor.getPose() == Pose.STANDING && actor.getDimensions(Pose.STANDING).height == 3, "Crawling state was not restored"); count++;
        actor.setSwimming(true); actor.setPose(Pose.SWIMMING);
        PhysicalCrawl.apply(actor, true); require(!actor.isSwimming(), "Dry crawl preserved the water-swimming flag"); count++;
        PhysicalCrawl.release(actor); require(actor.isSwimming() && actor.getPose() == Pose.SWIMMING, "Stopping playback did not restore the borrowed swimming state"); count++;
        actor.setSwimming(false); actor.setPose(Pose.STANDING);
        var explosive = TimelessAPI.getAllCommonGunIndex().stream().filter(e -> e.getValue().getBulletData().getGravity() > 0 && e.getValue().getBulletData().getExplosionData() != null && e.getValue().getBulletData().getExplosionData().isExplode()).findFirst().orElseThrow();
        var grenadeGun = GunItemBuilder.create().setId(explosive.getKey()).setAmmoCount(10).setAmmoInBarrel(true).build();
        owner.setItemSlot(EquipmentSlot.MAINHAND, grenadeGun); AttachmentPropertyManager.postChangeEvent(owner, grenadeGun);
        var grenade = new EntityKineticBullet(level, owner, grenadeGun, explosive.getValue().getGunData().getAmmoId(), explosive.getKey(), true, explosive.getValue().getGunData(), explosive.getValue().getBulletData());
        grenade.setDeltaMovement(0, 0.2, 1);
        var ballistics = ((KineticBulletAccess) grenade).bbstacz$captureBallistics(); ballistics.putInt("explosionDelayCount", 50);
        ((KineticBulletAccess) grenade).bbstacz$restoreBallistics(ballistics, grenade.position());
        TaczProjectileActionClip grenadeClip = TaczProjectileActionClip.capture(grenade, owner);
        TaczProjectileActionClip grenadeCopy = new TaczProjectileActionClip(); grenadeCopy.fromData(grenadeClip.toData());
        actor.setPos(owner.getX() + 10, owner.getY() + 10, owner.getZ()); actor.setYRot(owner.getYRot() + 90); grenadeCopy.applyAction(actor, null, null, null, 0);
        EntityKineticBullet playedGrenade = null;
        for (var entity : level.getAllEntities()) if (entity instanceof EntityKineticBullet bullet && bullet.getOwner() == actor && bullet != replayed) playedGrenade = bullet;
        require(playedGrenade != null && ((KineticBulletAccess) playedGrenade).bbstacz$captureBallistics().getFloat("explosionRadius") > 0, "Replay grenade has no real native explosion"); count++;
        require(ballistics.equals(((KineticBulletAccess) playedGrenade).bbstacz$captureBallistics()), "Grenade damage/fuse state was not retained"); count++;
        require(Math.abs(playedGrenade.getDeltaMovement().x + 1) < 1e-5 && Math.abs(playedGrenade.getDeltaMovement().z) < 1e-5, "Projectile velocity did not follow the new actor orientation"); count++;
        double vy = playedGrenade.getDeltaMovement().y; playedGrenade.tick();
        require(playedGrenade.getDeltaMovement().y < vy && !playedGrenade.isRemoved(), "Grenade did not follow native gravity/fuse physics"); count++;
        owner.setItemSlot(EquipmentSlot.MAINHAND, gun); AttachmentPropertyManager.postChangeEvent(owner, gun);
        grenade.discard(); playedGrenade.discard(); source.discard(); replayed.discard(); actor.discard();
        return count;
    }
    static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
