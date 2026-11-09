package mcaxolotl.bbstacz;

import com.tacz.guns.api.event.common.GunFirePostEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import mcaxolotl.bbstacz.actions.TaczFireActionClip;
import mcaxolotl.bbstacz.actions.TaczProjectileActionClip;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.BBSApi;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterActionClipsEvent;
import mchorse.bbs_mod.api.events.RegisterFormsEvent;
import mcaxolotl.bbstacz.forms.TaczForm;
import mchorse.bbs_mod.api.events.RegisterKeyframeFactoriesEvent;
import mcaxolotl.bbstacz.recording.FrameFactory;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import mcaxolotl.bbstacz.recording.PhysicalCrawl;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BBSTaczAddon implements BBSAddonMod {
    public static final String MOD_ID = "bbs_tacz";
    public static final Logger LOGGER = LoggerFactory.getLogger("BBS TACZ");
    public BBSTaczAddon() {
        BBSApi.requireVersion(MOD_ID, 2);
        mcaxolotl.bbstacz.network.F6ServerStart.register();
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PhysicalCrawl.clearServer());
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof EntityKineticBullet bullet && bullet.getOwner() instanceof ServerPlayer owner)
                BBSMod.getActions().addAction(owner, () -> TaczProjectileActionClip.capture(bullet, owner));
        });
        GunFirePostEvent.EVENT.register(event -> {
            if (event.getLogicalSide().isServer() && event.getShooter() instanceof ServerPlayer player) {
                BBSMod.getActions().addAction(player, () -> {
                    TaczFireActionClip clip = new TaczFireActionClip(); clip.itemStack.set(event.getGunItemStack().copy()); return clip;
                });
            }
        });
        LOGGER.info("BBS TACZ initialized (BBS addon API {}, native projectile capture)", BBSApi.VERSION);
    }
    @Subscribe public void registerForms(RegisterFormsEvent event) { event.forms.register(new Link(MOD_ID, "player"), TaczForm.class, null); }
    @Subscribe public void registerFactories(RegisterKeyframeFactoriesEvent event) { event.register("bbs_tacz:frame_v1", FrameFactory.INSTANCE); }
    @Subscribe public void registerActions(RegisterActionClipsEvent event) {
        event.factory.register(new Link(MOD_ID, "projectile"), TaczProjectileActionClip.class, new ClipFactoryData(Icons.BULLET, 0xeab55b));
        event.factory.register(new Link(MOD_ID, "fire"), TaczFireActionClip.class, new ClipFactoryData(Icons.BULLET, 0xff7448));
    }
}
