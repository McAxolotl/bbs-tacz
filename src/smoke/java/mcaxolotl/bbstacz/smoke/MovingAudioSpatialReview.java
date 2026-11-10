package mcaxolotl.bbstacz.smoke;

import mcaxolotl.bbstacz.client.RecordedAudio;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.WorldFilmController;
import net.minecraft.client.Minecraft;

/** Native actor cues must be spatial and follow motion, not remain relative to the spectator. */
public final class MovingAudioSpatialReview implements ReviewClient.Scenario {
    private WorldFilmController controller;
    @Override public boolean tick(Minecraft client,int tick) {
        if(tick==0) { controller=new WorldFilmController(AudioModeFixture.moving(client,false,false)); BBSModClient.getFilms().add(controller); }
        if(tick>=15) {
            var voices=RecordedAudio.voices(controller); ReviewClient.require(!voices.isEmpty(),"Moving world sound fixture has no native voice");
            var sound=voices.get(0); var actor=controller.getEntities().get(controller.film.replays.getList().get(0).getId());
            System.out.println("[REVIEW audio spatial] tick="+controller.getTick()+" relative="+sound.isRelative()+" source x="+sound.getX()+" actor x="+actor.getX());
            ReviewClient.require(!sound.isRelative(),"Third-person actor sound incorrectly remained relative to the spectator");
            ReviewClient.require(Math.abs(sound.getX()-actor.getX())<0.2,"Native sound source did not follow the moving replay actor"); return true;
        }
        return false;
    }
}
