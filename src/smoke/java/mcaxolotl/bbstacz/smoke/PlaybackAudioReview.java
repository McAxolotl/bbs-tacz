package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.sound.GunSoundInstance;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import mcaxolotl.bbstacz.client.RecordedAudio;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.mixin.client.SoundChannelAccess;
import mcaxolotl.bbstacz.mixin.client.SoundEngineAccess;
import mcaxolotl.bbstacz.mixin.client.SoundManagerAccess;
import mcaxolotl.bbstacz.recording.NativeSoundTimeline;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FirstPersonFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.openal.AL10;

/** Native GunSoundInstance play/pause/seek/resume/stop after a serialized take. No generated audio clips/assets. */
public final class PlaybackAudioReview implements ReviewClient.Scenario {
    private Film film;
    private GunSoundInstance inspected,voice;
    private FirstPersonFilmController playback;
    private NativeSoundTimeline.Cue reload;
    private int sourceState(Minecraft client,GunSoundInstance sound) throws Exception {
        var engine=((SoundManagerAccess)client.getSoundManager()).bbstacz$engine();
        var handle=((SoundEngineAccess)engine).bbstacz$channels().get(sound);
        ReviewClient.require(handle!=null,"Native replay sound acquired no OpenAL channel");
        var state=new CompletableFuture<Integer>(); handle.execute(channel->state.complete(AL10.alGetSourcei(((SoundChannelAccess)channel).bbstacz$source(),AL10.AL_SOURCE_STATE)));
        return state.get(3,TimeUnit.SECONDS);
    }
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if (tick==0) {
            var gun=GunItemBuilder.create().setId(new ResourceLocation("tacz","ak47")).setAmmoCount(30).setAmmoInBarrel(true).build();
            client.player.setItemSlot(EquipmentSlot.MAINHAND,gun); AttachmentPropertyManager.postChangeEvent(client.player,gun);
            film=new Film(); film.setId("bbs-tacz-native-audio-review-"+System.currentTimeMillis());
            var replay=new Replay("sound_player"); replay.form.set(new TaczForm()); replay.fp.set(true); film.replays.add(replay);
            BBSModClient.getFilms().startRecording(film,0,0,false); BBSModClient.getFilms().getRecorder().countdown=0;
        }
        if (tick==20) inspected=SoundPlayManager.playAnimationSound(client.player,new ResourceLocation("tacz","ak47/ak47_reload_mvt"),0.8f,1.15f,30);
        if (tick==22) SoundPlayManager.playClientSound(client.player,new ResourceLocation("tacz","ak47/ak47_shoot"),0.7f,0.95f,30);
        if (tick==26 && inspected!=null) inspected.setStop();
        if (tick==35) {
            var recorder=BBSModClient.getFilms().stopRecording();
            film.replays.getList().get(0).keyframes.copyOver(recorder.keyframes,0);
            var copy=new Film(); copy.fromData(film.toData()); copy.setId(film.getId());
            ((TaczReplayChannels)copy.replays.getList().get(0).keyframes).bbstacz$state().insert(80,mcaxolotl.bbstacz.recording.FrameSnapshot.EMPTY);
            var cues=NativeSoundTimeline.read(((TaczReplayChannels)copy.replays.getList().get(0).keyframes).bbstacz$sounds());
            ReviewClient.require(inspected!=null && cues.size()>=2,"Native sound cues did not survive film serialization");
            ReviewClient.require(copy.camera.getClips(AudioClip.class).isEmpty(),"Native sound was extracted to an audio clip");
            reload=cues.stream().filter(c->c.name().endsWith("reload_mvt")).findFirst().orElseThrow();
            ReviewClient.require(reload.end()-reload.tick()<8,"Native interruption was not serialized");
            client.player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            playback=new FirstPersonFilmController(copy); playback.tick=(int)reload.tick()+1; playback.paused=true; BBSModClient.getFilms().add(playback);
            System.out.println("[REVIEW native audio] serialized cues="+cues.size()+" reload ticks="+(reload.end()-reload.tick()));
        }
        if (tick==36) { RecordedAudio.playback(playback,0); ReviewClient.require(RecordedAudio.voices(playback).isEmpty(),"Paused seek started an audible cue"); }
        if (tick==37) { playback.paused=false; RecordedAudio.playback(playback,0); voice=RecordedAudio.voices(playback).stream().filter(s->s.getRegistryName().toString().equals(reload.name())).findFirst().orElseThrow(); }
        if (tick==38) {
            playback.paused=true; RecordedAudio.playback(playback,0);
            ReviewClient.require(voice.getSound().getPath().getPath().startsWith("tacz_sounds/"),"Replay used a baked audio asset instead of TACZ's native sound");
            int state=sourceState(client,voice); System.out.println("[REVIEW native audio] paused source state="+state+" film tick="+playback.getTick()+" source="+voice.getRegistryName()+" streaming="+voice.getSound().shouldStream());
            ReviewClient.require(state==AL10.AL_PAUSED,"Native replay source failed to pause (state="+state+")");
        }
        if (tick==39) {
            playback.paused=false; RecordedAudio.playback(playback,0);
            ReviewClient.require(sourceState(client,voice)==AL10.AL_PLAYING,"Native replay source failed to resume");
        }
        if (tick==40) {
            playback.paused=true; playback.tick=(int)Math.ceil(reload.end()+40); RecordedAudio.playback(playback,0);
            ReviewClient.require(RecordedAudio.voices(playback).isEmpty(),"Seeking outside cues kept stale gun sounds alive");
        }
        if (tick==41) { playback.tick=(int)reload.tick()+1; playback.paused=false; RecordedAudio.playback(playback,0); ReviewClient.require(!RecordedAudio.voices(playback).isEmpty(),"Backward seek failed to reconstruct native sound"); }
        if (tick==42) {
            BBSModClient.getFilms().remove(film.getId()); ReviewClient.require(RecordedAudio.voices(playback).isEmpty(),"Stopped film retained its native audio owner");
            System.out.println("[REVIEW native audio] OpenAL play/pause/seek/resume/stop PASS"); return true;
        }
        return false;
    }
}
