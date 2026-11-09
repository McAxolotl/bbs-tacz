package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mcaxolotl.bbstacz.forms.TaczForm;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/** Record actual TACZ sound calls through BBS's public take API and inspect the saved/loaded audio timeline. */
public final class PlaybackAudioReview implements ReviewClient.Scenario {
    private Film film;
    private com.tacz.guns.client.sound.GunSoundInstance inspected;
    private mchorse.bbs_mod.film.FirstPersonFilmController playback;
    private AudioClip shoot;
    private mchorse.bbs_mod.audio.SoundPlayer player;
    private net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext renderContext;
    public PlaybackAudioReview() { net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.LAST.register(context -> renderContext=context); }
    @Override public boolean tick(Minecraft client, int tick) throws Exception {
        if(tick==0) {
            var gun=GunItemBuilder.create().setId(new ResourceLocation("tacz","ak47")).setAmmoCount(30).setAmmoInBarrel(true).build();
            client.player.setItemSlot(EquipmentSlot.MAINHAND,gun); AttachmentPropertyManager.postChangeEvent(client.player,gun);
            film=new Film(); film.setId("bbs-tacz-audio-review-"+System.currentTimeMillis());
            var replay=new Replay("sound_player"); replay.form.set(new TaczForm()); replay.fp.set(true); film.replays.add(replay);
            BBSModClient.getFilms().startRecording(film,0,0,false); BBSModClient.getFilms().getRecorder().countdown=0;
        }
        if(tick==20) inspected=SoundPlayManager.playAnimationSound(client.player,new ResourceLocation("tacz","ak47/ak47_reload_mvt"),0.8f,1.15f,30);
        if(tick==22) SoundPlayManager.playClientSound(client.player,new ResourceLocation("tacz","ak47/ak47_shoot"),0.7f,0.95f,30);
        if(tick==26 && inspected!=null) inspected.setStop();
        if(tick==35) {
            var recorder=BBSModClient.getFilms().stopRecording();
            Film copy=new Film(); copy.fromData(recorder.film.toData()); copy.setId(film.getId());
            var clips=copy.camera.getClips(AudioClip.class);
            System.out.println("[REVIEW audio] soundInstance="+inspected+" saved audio clips="+clips.size());
            ReviewClient.require(inspected!=null,"Native TACZ test sound resource did not resolve");
            ReviewClient.require(clips.size()>=2,"No TACZ audio was serialized into the replay (captured="+clips.size()+")");
            ReviewClient.require(clips.stream().allMatch(c->c.audio.get()!=null&&c.duration.get()>0),"Invalid saved TACZ audio clip");
            var reload=clips.stream().filter(c->c.title.get().contains("reload_mvt")).findFirst().orElseThrow();
            var buffer=BBSModClient.getSounds().get(reload.audio.get(),false);
            ReviewClient.require(buffer!=null && buffer.getDuration()<0.38f,"Native interruption was not baked into the sound tail");
            shoot=clips.stream().filter(c->c.title.get().contains("ak47_shoot")).findFirst().orElseThrow();
            var out=java.nio.file.Path.of("review-recorded-tacz-audio.wav");
            ReviewClient.require(mchorse.bbs_mod.audio.AudioRenderer.renderAudio(out.toFile(),clips,copy.calculateDuration(),48000,0,0),"BBS offline audio mixing failed");
            mchorse.bbs_mod.audio.Wave mixed; try(var input=java.nio.file.Files.newInputStream(out)) { mixed=new mchorse.bbs_mod.audio.wav.WaveReader().read(input); }
            long nonzero=0; for(byte b:mixed.data) if(b!=0) nonzero++;
            ReviewClient.require(nonzero>1000,"Serialized audio rendered only silence");
            copy.replays.getList().get(0).keyframes.copyOver(recorder.keyframes,0);
            for(var clip:clips) ReviewClient.require(BBSModClient.getSounds().get(clip.audio.get(),false)!=null,"Serialized audio asset failed to load");
            playback=new mchorse.bbs_mod.film.FirstPersonFilmController(copy); playback.tick=shoot.tick.get()+1; playback.paused=true;
            BBSModClient.getFilms().add(playback);
            System.out.println("[REVIEW audio] baked reload seconds="+buffer.getDuration()+" offline nonzero bytes="+nonzero);
        }
        if(tick==36) {
            ReviewClient.require(renderContext!=null,"No native world render context was observed");
            playback.render(renderContext);
            player=BBSModClient.getSounds().getPlayers().stream().filter(p->p.getOwner()==shoot).findFirst().orElseThrow();
            playback.paused=false; playback.render(renderContext);
            ReviewClient.require(player.isPlaying() && org.lwjgl.openal.AL10.alGetSourcef(player.getSource(),org.lwjgl.openal.AL10.AL_GAIN)>0,"Native BBS audio playback is not audible");
            playback.paused=true; playback.render(renderContext);
            ReviewClient.require(player.isPaused(),"Paused world replay keeps playing TACZ audio");
            playback.tick=shoot.tick.get()+2; playback.render(renderContext);
            ReviewClient.require(player.isPaused() && Math.abs(player.getPlaybackPosition()-0.1f)<0.06f,"Paused seek did not reposition the audio source");
            playback.paused=false; playback.render(renderContext);
            ReviewClient.require(player.isPlaying(),"Resuming the replay did not resume TACZ audio");
            playback.paused=true; playback.tick=shoot.tick.get()+shoot.duration.get(); playback.render(renderContext);
            ReviewClient.require(!player.isPlaying(),"Seek outside the sound clip left stale TACZ audio running");
            BBSModClient.getFilms().remove(playback.film.getId());
            ReviewClient.require(BBSModClient.getSounds().getPlayers().stream().noneMatch(p->p.getOwner()==shoot),"Stopped replay retained its OpenAL audio owner");
            System.out.println("[REVIEW audio] runtime play/pause/seek/resume/stop passed");
            return true;
        }
        return false;
    }
}
