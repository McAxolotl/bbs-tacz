package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.client.sound.SoundPlayManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.NativeSoundTimeline;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
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

/** Legacy WAV compatibility only: retakes mute/replace their own old range, preserve user tracks and record new native cues. */
public final class AudioRetakeReview implements ReviewClient.Scenario {
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick!=0) return false;
        var film=new Film(); film.setId("bbs-tacz-retake-review-"+System.currentTimeMillis());
        var replay=new Replay("recorded_player"); replay.form.set(new TaczForm()); film.replays.add(replay);
        var old=(AudioClip)film.camera.getFactory().create(Link.bbs("audio")); old.tick.set(0); old.duration.set(30); old.title.set("legacy TACZ sound");
        String legacyPath="audio/bbs_tacz/"+UUID.nameUUIDFromBytes(replay.getId().getBytes(StandardCharsets.UTF_8))+"/legacy-fixture.wav";
        old.audio.set(Link.assets(legacyPath)); var original=BBSMod.getAssetsPath(legacyPath).toPath(); Files.createDirectories(original.getParent());
        var wave=new mchorse.bbs_mod.audio.Wave(1,1,48000,16,new byte[144000]); mchorse.bbs_mod.audio.wav.WaveWriter.write(original.toFile(),wave); film.camera.addClip(old);
        var manual=(AudioClip)old.copy(); manual.title.set("user audio"); manual.audio.set(Link.assets("audio/review-user-"+System.currentTimeMillis()+".wav"));
        Files.copy(original,BBSMod.getAssetsPath(manual.audio.get().path).toPath()); film.camera.addClip(manual);
        var otherReplay=new Replay("other_player"); otherReplay.form.set(new TaczForm()); film.replays.add(otherReplay);
        var other=(AudioClip)old.copy(); other.title.set("other replay audio");
        String path="audio/bbs_tacz/"+UUID.nameUUIDFromBytes(otherReplay.getId().getBytes(StandardCharsets.UTF_8))+"/other.wav";
        other.audio.set(Link.assets(path)); Files.createDirectories(BBSMod.getAssetsPath(path).toPath().getParent()); Files.copy(original,BBSMod.getAssetsPath(path).toPath()); film.camera.addClip(other);
        int start=5,end=11; BBSModClient.getFilms().startRecording(film,0,start,false);
        var recorder=BBSModClient.getFilms().getRecorder(); recorder.countdown=0; recorder.tick=start+1;
        var context=recorder.getContext(); context.setup(start+1,0); context.apply(old,new Position()); AudioClientClip.manageSounds(context);
        ReviewClient.require(BBSModClient.getSounds().getPlayers().stream().noneMatch(p->p.getOwner()==old&&p.isPlaying()),"Old WAV take monitored over native gun");
        SoundPlayManager.playClientSound(client.player,new ResourceLocation("tacz","ak47/ak47_shoot"),0.8f,1,30);
        recorder.tick=end; BBSModClient.getFilms().stopRecording();
        var kept=film.camera.getClips(AudioClip.class);
        ReviewClient.require(kept.contains(manual)&&kept.contains(other),"Retake removed unrelated audio");
        var portions=kept.stream().filter(c->c.audio.get().equals(old.audio.get())).toList(); ReviewClient.require(portions.size()==2,"Legacy retake lost outside portions");
        ReviewClient.require(portions.stream().anyMatch(c->c.tick.get()==0&&c.duration.get()==start),"Legacy head was not preserved");
        ReviewClient.require(portions.stream().anyMatch(c->c.tick.get()==end&&c.offset.get()==end),"Legacy tail was not preserved");
        ReviewClient.require(NativeSoundTimeline.read(((TaczReplayChannels)recorder.keyframes).bbstacz$sounds()).size()==1,"New take did not record a native cue");
        ReviewClient.require(kept.size()==4,"New take appended an extracted audio clip"); return true;
    }
}
