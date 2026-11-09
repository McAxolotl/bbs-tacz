package mcaxolotl.bbstacz.smoke;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.WorldFilmController;
import mchorse.bbs_mod.film.FrozenFilmController;
import net.minecraft.client.Minecraft;

/** The real Ctrl hotkey starts the native server/client film together, rather than beginning actions during client load. */
public final class CtrlStartupReview extends HotkeyWorkflowReview {
    private boolean checked;
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(phase==4 && !checked) {
            var controller=BBSModClient.getFilms().getController(film.getId());
            if(controller instanceof WorldFilmController && !(controller instanceof FrozenFilmController)) {
                int server=client.getSingleplayerServer().submit(()-> { var action=BBSMod.getActions().getPlayer(film.getId()); return action==null?-1:action.tick; }).get();
                System.out.println("[REVIEW Ctrl startup] first client/server ticks="+controller.getTick()+"/"+server);
                ReviewClient.require(controller.getTick()<=1&&server>=0&&server<=controller.getTick()+1,"Ctrl server actions advanced before the client's opening frame"); checked=true;
            }
        }
        boolean done=super.tick(client,tick); if(done) ReviewClient.require(checked,"Ctrl startup ownership was never observed"); return done;
    }
}
