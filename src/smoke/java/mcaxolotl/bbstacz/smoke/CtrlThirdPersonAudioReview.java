package mcaxolotl.bbstacz.smoke;

import mcaxolotl.bbstacz.client.RecordedAudio;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.WorldFilmController;
import mchorse.bbs_mod.film.FrozenFilmController;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Actual Ctrl playback with fp=false, native cues and a visibly moving stub actor. */
public final class CtrlThirdPersonAudioReview implements ReviewClient.Scenario {
    private Film film;
    private double startX;
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick==0) {
            film=AudioModeFixture.moving(client,false); startX=client.player.getX();
            mchorse.bbs_mod.ui.ContentType.FILMS.getRepository().save(film.getId(),film.toData().asMap());
            BBSModClient.getDashboard().getPanel(UIFilmPanel.class).fill(film);
            client.getSingleplayerServer().submit(()->BBSMod.getFilms().save(film.getId(),film.toData().asMap())).get();
        }
        if(tick==10) client.setScreen(null);
        if(tick==30) F4ClockReview.key(GLFW.GLFW_KEY_RIGHT_CONTROL);
        var current=BBSModClient.getFilms().getController(film.getId());
        if(tick==45) System.out.println("[REVIEW Ctrl TP setup] controllers="+BBSModClient.getFilms().getControllers().stream().map(c->c.getClass().getSimpleName()+":"+c.getTick()).toList()+" BBS server="+mchorse.bbs_mod.network.ClientNetwork.isIsBBSModOnServer());
        ReviewClient.require(tick<160,"Ctrl TP fixture never started playback");
        if(tick>25 && current instanceof WorldFilmController world && world.getTick()>8) {
            var actor=world.getEntities().get(world.film.replays.getList().get(0).getId());
            System.out.println("[REVIEW Ctrl TP audio] tick="+world.getTick()+" actor="+actor.getClass().getSimpleName()+" movement="+Math.abs(actor.getX()-startX)+" voices="+RecordedAudio.voices(world).size());
            ReviewClient.require(Math.abs(actor.getX()-startX)>0.1,"Ctrl TP actor did not move");
            ReviewClient.require(!RecordedAudio.voices(world).isEmpty(),"Ctrl TP playback is silent despite native TACZ cues"); return true;
        }
        return false;
    }
}
