package mcaxolotl.bbstacz.actions;

import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunFire;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.types.item.ItemActionClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.world.entity.LivingEntity;

/** Server notification only. Projectiles have separate clips, so a burst cannot be emitted twice. */
public final class TaczFireActionClip extends ItemActionClip {
    @Override public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick) {
        if (actor != null && !actor.level().isClientSide) NetworkHandler.sendToTrackingEntity(new ServerMessageGunFire(actor.getId(), itemStack.get().copy()), actor);
    }
    @Override protected Clip create() { return new TaczFireActionClip(); }
}
