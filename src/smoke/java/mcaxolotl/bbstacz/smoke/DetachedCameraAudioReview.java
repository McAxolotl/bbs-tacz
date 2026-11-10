package mcaxolotl.bbstacz.smoke;

import mcaxolotl.bbstacz.client.RecordedAudio;
import mcaxolotl.bbstacz.recording.NativeSoundTimeline;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.overwrite.IdleClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Films;
import net.minecraft.client.Minecraft;

/** Real BBS movie camera, not a manually moved MainCamera: spectator/actor are separated by 200 metres. */
public final class DetachedCameraAudioReview implements ReviewClient.Scenario {
    private Film film; private boolean far,near;
    @Override public boolean tick(Minecraft client,int tick) {
        if(tick==0) {
            film=AudioModeFixture.moving(client,false,false); for(var clip:new java.util.ArrayList<>(film.camera.get())) film.camera.remove(clip);
            var replay=film.replays.getList().get(0); replay.keyframes.x.insert(0,client.player.getX()+200); replay.keyframes.x.insert(100,client.player.getX()+208);
            var distant=new IdleClip(); distant.tick.set(0); distant.duration.set(30); distant.position.get().point.set(client.player.getX(),client.player.getY()+2,client.player.getZ()); film.camera.addClip(distant);
            var close=new IdleClip(); close.tick.set(30); close.duration.set(70); close.position.get().point.set(client.player.getX()+203,client.player.getY()+2,client.player.getZ()); film.camera.addClip(close);
            var sounds=((TaczReplayChannels)replay.keyframes).bbstacz$sounds(); sounds.removeAll();
            NativeSoundTimeline.append(sounds,10,NativeSoundTimeline.play("far","tacz:ak47/ak47_reload_mvt",0.8f,1,30,false,0,false,true,15));
            NativeSoundTimeline.append(sounds,45,NativeSoundTimeline.play("near","tacz:ak47/ak47_reload_mvt",0.8f,1,30,false,0,false,true,30));
            Films.playFilm(film,true);
        }
        var controller=BBSModClient.getFilms().getController(film.getId());
        if(controller!=null) {
            var voices=RecordedAudio.voices(controller);
            if(controller.getTick()>=12&&controller.getTick()<25&&!voices.isEmpty()) { ReviewClient.require(voices.get(0).getVolume()==0,"Distant spatial actor ignored the real movie listener"); far=true; }
            if(controller.getTick()>=50&&controller.getTick()<70&&!voices.isEmpty()) {
                var sound=voices.get(0);
                System.out.println("[REVIEW detached listener] camera="+client.gameRenderer.getMainCamera().getPosition()+" spectator="+client.player.position()+" source="+sound.getX()+" volume="+sound.getVolume());
                ReviewClient.require(sound.getVolume()>0.4,"Movie camera is close to the actor, but native audio still attenuates from the distant spectator"); near=true;
            }
        }
        if(near&&far) { Films.stopFilm(film.getId()); return true; }
        ReviewClient.require(tick<160,"Detached camera fixture failed to observe both distances: far="+far+" near="+near); return false;
    }
}
