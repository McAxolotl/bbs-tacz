package mcaxolotl.bbstacz.network;

import mcaxolotl.bbstacz.forms.TaczForm;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.utils.PermissionUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

/** Create and hold a normal native playback atomically, before its first server action tick. No rewind/re-fire. */
public final class F6ServerStart {
    public static final ResourceLocation ID = new ResourceLocation("bbs_tacz", "f6_start");
    private F6ServerStart() {}
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buffer, responder) -> {
            String id = buffer.readUtf(256);
            boolean ctrl=buffer.isReadable() && buffer.readBoolean();
            if (id.isBlank() || id.startsWith("/") || id.contains("..") || id.contains("\\")) return;
            server.execute(() -> {
                if (!PermissionUtils.arePanelsAllowed(server, player)) return;
                var film = BBSMod.getFilms().load(id);
                boolean tacz=film!=null&&film.replays.getList().stream().anyMatch(track->track.enabled.get()&&track.form.get() instanceof TaczForm);
                if (!tacz) return;
                BBSMod.getActions().stop(id);
                ServerNetwork.sendPlayFilm(player, player.serverLevel(), id, !ctrl&&!film.hasFirstPerson()&&!film.camera.get().isEmpty());
                var playback = BBSMod.getActions().getPlayer(id);
                if (playback != null) playback.playing = false;
                if(ctrl) CtrlClockSync.watch(player,playback);
            });
        });
    }
}
