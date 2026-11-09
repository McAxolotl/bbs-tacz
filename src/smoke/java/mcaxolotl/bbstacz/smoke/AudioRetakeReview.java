package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.client.sound.SoundPlayManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import mcaxolotl.bbstacz.forms.TaczForm;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.misc.AudioClientClip;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.resources.Link;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Rerecording replaces only its own audio interval and does not monitor the old take over live gun sounds. */
public final class AudioRetakeReview implements ReviewClient.Scenario {
    private Film film;
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick==0) {
            film=new Film(); film.setId("bbs-tacz-retake-review-"+System.currentTimeMillis());
            var replay=new Replay("recorded_player"); replay.form.set(new TaczForm()); film.replays.add(replay);
            BBSModClient.getFilms().startRecording(film,0,0,false); BBSModClient.getFilms().getRecorder().countdown=0;
        }
        if(tick==5) SoundPlayManager.playClientSound(client.player,new ResourceLocation("tacz","ak47/ak47_shoot"),0.8f,1,30);
        if(tick==25) {
            BBSModClient.getFilms().stopRecording();
            AudioClip old=film.camera.getClips(AudioClip.class).stream().filter(c->c.title.get().contains("ak47_shoot")).findFirst().orElseThrow();
            var originalFile=BBSMod.getAssetsPath(old.audio.get().path).toPath();
            var manual=(AudioClip)old.copy(); manual.title.set("user audio");
            manual.audio.set(Link.assets("audio/review-user-"+System.currentTimeMillis()+".wav"));
            Files.copy(originalFile,BBSMod.getAssetsPath(manual.audio.get().path).toPath()); film.camera.addClip(manual);
            var otherReplay=new Replay("other_player"); otherReplay.form.set(new TaczForm()); film.replays.add(otherReplay);
            var other=(AudioClip)old.copy(); other.title.set("other replay audio");
            String otherPath="audio/bbs_tacz/"+UUID.nameUUIDFromBytes(otherReplay.getId().getBytes(StandardCharsets.UTF_8))+"/review-other-"+System.currentTimeMillis()+".wav";
            other.audio.set(Link.assets(otherPath)); Files.createDirectories(BBSMod.getAssetsPath(otherPath).toPath().getParent()); Files.copy(originalFile,BBSMod.getAssetsPath(otherPath).toPath()); film.camera.addClip(other);
            int start=old.tick.get()+5, end=start+6;
            BBSModClient.getFilms().startRecording(film,0,start,false);
            var recorder=BBSModClient.getFilms().getRecorder(); recorder.countdown=0; recorder.tick=start+1;
            var context=recorder.getContext(); context.setup(start+1,0); context.apply(old,new Position()); AudioClientClip.manageSounds(context);
            boolean echo=BBSModClient.getSounds().getPlayers().stream().anyMatch(p->p.getOwner()==old && p.isPlaying());
            System.out.println("[REVIEW retake] old take audible while recording="+echo);
            ReviewClient.require(!echo,"The old TACZ audio track is played over a new take of the same replay");
            recorder.tick=end; BBSModClient.getFilms().stopRecording();
            var kept=film.camera.getClips(AudioClip.class);
            ReviewClient.require(kept.contains(manual)&&kept.contains(other),"Retake removed unrelated audio");
            var portions=kept.stream().filter(c->c.audio.get().equals(old.audio.get())).toList();
            ReviewClient.require(portions.size()==2,"Retake did not preserve both outside portions of the old cue");
            ReviewClient.require(portions.stream().anyMatch(c->c.tick.get()==old.tick.get()&&c.tick.get()+c.duration.get()==start),"Retake lost the old cue's head");
            ReviewClient.require(portions.stream().anyMatch(c->c.tick.get()==end&&c.offset.get()==old.offset.get()+end-old.tick.get()),"Retake lost or mistimed the old cue's tail");
            return true;
        }
        return false;
    }
}
