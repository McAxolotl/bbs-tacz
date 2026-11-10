package mcaxolotl.bbstacz.smoke;

import mcaxolotl.bbstacz.client.RecordedAudio;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.minecraft.client.Minecraft;

/** A moving actor with native cues must be audible through the actual editor runner, not a manually called audio API. */
public class EditorNativeAudioReview implements ReviewClient.Scenario {
    protected boolean fp() { return false; }
    private UIFilmPanel panel;
    private double startX;
    @Override public boolean tick(Minecraft client,int tick) {
        if(tick==0) {
            var film=AudioModeFixture.moving(client,fp()); var dashboard=BBSModClient.getDashboard(); panel=dashboard.getPanel(UIFilmPanel.class);
            panel.fill(film); dashboard.getPanels().setPanel(panel); UIScreen.open(dashboard);
            panel.getController().createEntities(); panel.getRunner().setWork(film.camera);
            panel.getRunner().setCursor(0); startX=client.player.getX();
        }
        if(tick==20) System.out.println("[REVIEW editor setup] running="+panel.getRunner().isRunning()+" ticks="+panel.getRunner().ticks+" screen="+client.screen.getClass().getSimpleName()+" paused MC="+client.isPaused());
        ReviewClient.require(tick<160,"Editor fixture runner never advanced");
        if(tick==10) panel.getRunner().setPlaying(true);
        if(tick>=25 && panel.getRunner().ticks>=8) {
            var controller=panel.getController().editorController;
            var actor=controller.getEntities().get(controller.film.replays.getList().get(0).getId());
            System.out.println("[REVIEW editor audio] fp="+fp()+" runner="+panel.getRunner().isRunning()+" tick="+controller.getTick()+" paused="+controller.paused+" actor="+actor.getClass().getSimpleName()+" movement="+Math.abs(actor.getX()-startX)+" voices="+RecordedAudio.voices(controller).size());
            ReviewClient.require(Math.abs(actor.getX()-startX)>0.1,"Editor fixture did not move the actor");
            ReviewClient.require(!RecordedAudio.voices(controller).isEmpty(),"Moving editor playback is silent despite native TACZ cues");
            return true;
        }
        return false;
    }
}
