package mcaxolotl.bbstacz.client;

import com.tacz.guns.api.event.common.GunFireEvent;
import mcaxolotl.bbstacz.config.TaczConfig;
import mcaxolotl.bbstacz.recording.PhysicalCrawl;
import mcaxolotl.bbstacz.recording.ReplayBindings;
import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterFormRenderersEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormEditorsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormSectionsEvent;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.client.forms.TaczFormRenderer;
import mcaxolotl.bbstacz.client.forms.TaczFormSection;
import mcaxolotl.bbstacz.client.ui.UITaczForm;
import mchorse.bbs_mod.api.client.events.FilmEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;

public final class BBSTaczClient implements BBSAddonMod {
    public BBSTaczClient() {
        TaczConfig.load();
        FilmEvents.CREATED.register(controller -> { FirstPersonPlayback.watch(controller); ReplayClock.watch(controller); });
        FilmEvents.TICK_AFTER.register((controller, tick) -> {
            FirstPersonPlayback.watch(controller); ReplayClock.watch(controller);
            var target = FirstPersonPlayback.target(0);
            if (target != null && Minecraft.getInstance().player != null) PhysicalCrawl.apply(Minecraft.getInstance().player, target.frame().crawling());
        });
        FilmEvents.SHUTDOWN.register(controller -> {
            controller.getEntities().values().forEach(entity -> { ReplayBindings.release(entity); ThirdPersonShells.release(entity); });
            FirstPersonPlayback.stop(controller); ReplayClock.stop(controller);
            if (Minecraft.getInstance().player != null) PhysicalCrawl.release(Minecraft.getInstance().player);
        });
        GunFireEvent.CALLBACK.register(event -> {
            if (!event.getLogicalSide().isClient()) return;
            if (event.getShooter() == Minecraft.getInstance().player) { MirrorParticles.onShoot(event.getGunItemStack()); LiveCapture.onFire(); }
            ThirdPersonShells.remoteFire(event.getShooter(), event.getGunItemStack());
        });
        com.tacz.guns.api.event.common.GunShootEvent.CALLBACK.register(event -> { if (event.getLogicalSide().isClient()) ActorAnimations.event(event.getShooter(), event.getGunItemStack(), "fire"); });
        com.tacz.guns.api.event.common.GunReloadEvent.CALLBACK.register(event -> { if (event.getLogicalSide().isClient()) ActorAnimations.event(event.getEntity(), event.getGunItemStack(), "reload"); });
        com.tacz.guns.api.event.common.GunMeleeEvent.CALLBACK.register(event -> { if (event.getLogicalSide().isClient()) ActorAnimations.event(event.getShooter(), event.getGunItemStack(), "melee"); });
        com.tacz.guns.api.event.common.GunDrawEvent.CALLBACK.register(event -> { if (event.getLogicalSide().isClient()) ActorAnimations.event(event.getEntity(), event.getCurrentGunItem(), "draw"); });
        WorldRenderEvents.LAST.register(ThirdPersonShells::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }
    @Subscribe public void renderers(RegisterFormRenderersEvent event) { event.register(TaczForm.class, TaczFormRenderer::new); }
    @Subscribe public void editors(RegisterFormEditorsEvent event) { event.register(TaczForm.class, UITaczForm::new); }
    @Subscribe public void palette(RegisterFormSectionsEvent event) { event.register(TaczFormSection::new); }
    public static void clear() {
        LiveCapture.clear(); CommandJournal.clear(); ActorAnimations.clear(); FirstPersonPlayback.clear(); ThirdPersonPose.clear();
        NativeRenderScope.clear(); MirrorParticles.clear(); ReplayClock.clear(); ShellHistory.clear(); ThirdPersonShells.clear(); ShellIds.clear(); PhysicalCrawl.clearClient(); ReplayBindings.clear();
    }
}
