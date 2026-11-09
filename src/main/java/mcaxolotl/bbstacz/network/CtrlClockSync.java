package mcaxolotl.bbstacz.network;

import java.util.HashMap;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.ActionPlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** The server's action/physics time is authoritative for ordinary Ctrl world playback, including slow client renders. */
public final class CtrlClockSync {
    public static final ResourceLocation ID=new ResourceLocation("bbs_tacz","ctrl_clock");
    private record Session(ServerPlayer player,ActionPlayer action) {}
    private static final HashMap<String,Session> SESSIONS=new HashMap<>();
    private CtrlClockSync() {}
    public static void watch(ServerPlayer player,ActionPlayer action) { if(action!=null) SESSIONS.put(action.film.getId(),new Session(player,action)); }
    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server->SESSIONS.clear());
        ServerTickEvents.END_SERVER_TICK.register(server->{
            var iterator=SESSIONS.entrySet().iterator();
            while(iterator.hasNext()) {
                var entry=iterator.next(); var session=entry.getValue();
                if(session.player.isRemoved()) { iterator.remove(); continue; }
                if(BBSMod.getActions().getPlayer(entry.getKey())!=session.action) {
                    // Natural ActionManager completion removes the action before this hook.
                    // Send the terminal tick too, or an authoritative client would stay one tick short forever.
                    if(session.action.tick>=session.action.film.calculateDuration()) {
                        var end=PacketByteBufs.create(); end.writeUtf(entry.getKey()); end.writeVarInt(session.action.film.calculateDuration()); end.writeBoolean(false);
                        ServerPlayNetworking.send(session.player,ID,end);
                    }
                    iterator.remove(); continue;
                }
                var packet=PacketByteBufs.create(); packet.writeUtf(entry.getKey()); packet.writeVarInt(Math.max(0,session.action.tick)); packet.writeBoolean(session.action.playing);
                ServerPlayNetworking.send(session.player,ID,packet);
            }
        });
    }
}
