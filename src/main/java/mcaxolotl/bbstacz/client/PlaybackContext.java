package mcaxolotl.bbstacz.client;

import mcaxolotl.bbstacz.client.forms.TaczFormRenderer;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.FrameSampler;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.film.controller.FilmEditorController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Editor playback uses its visible runner; a BBS stub is an actor, not a missing Minecraft entity. */
public final class PlaybackContext {
    private PlaybackContext() {}
    public static boolean playing(BaseFilmController controller) {
        if (Minecraft.getInstance().isPaused()) return false;
        if (controller instanceof FilmEditorController) {
            var panel=FirstPersonPlayback.editorPanel(controller);
            return panel!=null&&panel.getRunner().isRunning();
        }
        return !controller.paused;
    }
    public static boolean firstPerson(BaseFilmController controller,Replay replay) {
        if (replay!=controller.film.getFirstPersonReplay() || !mcaxolotl.bbstacz.config.TaczConfig.INSTANCE.firstPersonReplay) return false;
        if (controller instanceof FilmEditorController) {
            var panel=FirstPersonPlayback.editorPanel(controller);
            return panel!=null && panel.getController().getPovMode()==mchorse.bbs_mod.ui.film.controller.UIFilmController.CAMERA_MODE_FIRST_PERSON;
        }
        return true;
    }
    public static Entity soundSource(BaseFilmController controller,Replay replay,IEntity actor,float time,float delta) {
        if (firstPerson(controller,replay)) return Minecraft.getInstance().player;
        if (actor instanceof MCEntity mc) return mc.getMcEntity();
        if (actor!=null&&actor.getForm() instanceof TaczForm form&&FormUtilsClient.getRenderer(form) instanceof TaczFormRenderer renderer) {
            var player=renderer.visualPlayer();
            if (player!=null) player.sync(actor,FrameSampler.sample(((TaczReplayChannels)replay.keyframes).bbstacz$state(),time),delta);
            return player;
        }
        return null;
    }
}
