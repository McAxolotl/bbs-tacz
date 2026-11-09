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
    private static final Object CAMERA_CRAWL = new Object();
    private static void syncCameraCrawl() {
        var player = Minecraft.getInstance().player; if (player == null) return;
        var target = FirstPersonPlayback.target(0);
        if (target != null) PhysicalCrawl.apply(player, CAMERA_CRAWL, target.frame().crawling());
        else PhysicalCrawl.release(player, CAMERA_CRAWL);
    }
    public BBSTaczClient() {
        TaczConfig.load();
        FilmEvents.CREATED.register(controller -> { FirstPersonPlayback.watch(controller); ReplayClock.watch(controller); });
        FilmEvents.TICK_AFTER.register((controller, tick) -> {
            FirstPersonPlayback.watch(controller); ReplayClock.watch(controller);
            syncCameraCrawl();
        });
        FilmEvents.SHUTDOWN.register(controller -> {
            FirstPersonPlayback.stop(controller); ReplayClock.stop(controller);
            controller.getEntities().values().forEach(entity -> { ReplayBindings.release(entity); ThirdPersonShells.release(entity); });
            syncCameraCrawl();
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
        F6Starts.clear(); RecordedAudio.clear(); SkinTextureBridge.clear(); LiveCapture.clear(); CommandJournal.clear(); ActorAnimations.clear(); FirstPersonPlayback.clear(); ThirdPersonPose.clear();
        NativeRenderScope.clear(); MirrorParticles.clear(); ReplayClock.clear(); ShellHistory.clear(); ThirdPersonShells.clear(); ShellIds.clear(); PhysicalCrawl.clearClient(); ReplayBindings.clear();
    }
}
