package mcaxolotl.bbstacz.client;

import java.util.HashMap;
import mchorse.bbs_mod.film.WorldFilmController;
import mchorse.bbs_mod.actions.ActionState;
import mchorse.bbs_mod.network.ClientNetwork;

/** Ctrl playback, like F6, may not run its server actions before the client has rendered its opening frame. */
public final class CtrlPlaybackStarts {
    private record Pending(F6Starts.Ticket ticket,long created,WorldFilmController controller) {}
    private static final HashMap<String,Pending> PENDING=new HashMap<>();
    private CtrlPlaybackStarts() {}
    public static void request(String id) { cancel(id); PENDING.put(id,new Pending(F6Starts.request(id),System.currentTimeMillis(),null)); }
    public static boolean pending(String id) {
        var request=PENDING.get(id); if(request!=null&&System.currentTimeMillis()-request.created>30_000) { cancel(id); return false; } return request!=null;
    }
    public static void cancel(String id) { var request=PENDING.remove(id); if(request!=null) F6Starts.cancel(request.ticket); }
    public static void ready(WorldFilmController controller) {
        String id=controller.film.getId(); var request=PENDING.get(id);
        if(request!=null) PENDING.put(id,new Pending(request.ticket,request.created,controller));
    }
    public static void shutdown(mchorse.bbs_mod.film.BaseFilmController controller) {
        var request=PENDING.get(controller.film.getId());
        if(request!=null && request.controller==controller) cancel(controller.film.getId());
    }
    public static void rendered(WorldFilmController controller) {
        String id=controller.film.getId(); var request=PENDING.get(id); if(request==null || request.controller!=controller) return;
        PENDING.remove(id);
        controller.paused=false;
        if(ClientNetwork.isIsBBSModOnServer()) ClientNetwork.sendActionState(id,ActionState.PLAY,0);
    }
    public static void clear() { PENDING.values().forEach(request->F6Starts.cancel(request.ticket)); PENDING.clear(); }
}
