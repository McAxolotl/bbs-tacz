package mcaxolotl.bbstacz.network;

import java.util.HashMap;
import java.util.UUID;
import mchorse.bbs_mod.utils.PermissionUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** TACZ action stamps follow the same recorded pose cursor, not an independent server tick counter. */
public final class RecordCursorSync {
    public static final ResourceLocation ID=new ResourceLocation("bbs_tacz","record_cursor");
    private record Cursor(String film,int tick,long updated) {}
    private static final HashMap<UUID,Cursor> CURSORS=new HashMap<>();
    private RecordCursorSync() {}
    public static void reset(ServerPlayer player) { CURSORS.remove(player.getUUID()); }
    public static Integer time(ServerPlayer player,String film) {
        var cursor=CURSORS.get(player.getUUID()); return cursor!=null&&cursor.film.equals(film)&&System.currentTimeMillis()-cursor.updated<5000?cursor.tick:null;
    }
    public static void register() {
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->reset(handler.player));
        ServerLifecycleEvents.SERVER_STOPPING.register(server->CURSORS.clear());
        ServerPlayNetworking.registerGlobalReceiver(ID,(server,player,handler,packet,sender)-> {
            String film=packet.readUtf(256); int tick=packet.readVarInt();
            if(tick<0||tick>100_000_000||film.isBlank()||film.contains("..")) return;
            server.execute(()-> { if(PermissionUtils.arePanelsAllowed(server,player)) CURSORS.put(player.getUUID(),new Cursor(film,tick,System.currentTimeMillis())); });
        });
    }
}
