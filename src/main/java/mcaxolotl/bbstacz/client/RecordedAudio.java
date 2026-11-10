package mcaxolotl.bbstacz.client;

import com.tacz.guns.client.sound.GunSoundInstance;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import mcaxolotl.bbstacz.BBSTaczAddon;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.mixin.client.NativeSoundPlayAccess;
import mcaxolotl.bbstacz.mixin.client.SoundChannelAccess;
import mcaxolotl.bbstacz.mixin.client.SoundEngineAccess;
import mcaxolotl.bbstacz.mixin.client.SoundManagerAccess;
import mcaxolotl.bbstacz.recording.AudioCursor;
import mcaxolotl.bbstacz.recording.NativeSoundTimeline;
import mcaxolotl.bbstacz.recording.ReplayBindings;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.audio.ogg.VorbisReader;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.film.WorldFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.resources.Link;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

/** Record/replay TACZ's native sound commands. Video audio is captured by BBS's Minecraft sound listener. */
public final class RecordedAudio {
    private static Take take;
    private static boolean emitting;
    private static final Map<ResourceLocation,Float> LENGTHS=new HashMap<>();
    private static final Map<BaseFilmController,Map<Replay,Playback>> PLAYBACKS=new WeakHashMap<>();
    private static long playedCues;
    private RecordedAudio() {}
    private static final class Take {
        final Recorder recorder;
        final Replay replay;
        final String id=UUID.randomUUID().toString();
        final Map<SoundInstance,String> active=new IdentityHashMap<>();
        int sequence;
        Take(Recorder recorder,Replay replay) { this.recorder=recorder; this.replay=replay; }
    }
    private static final class Voice {
        final NativeSoundTimeline.Cue cue;
        final GunSoundInstance sound;
        final Entity source;
        volatile boolean pendingSeek=true;
        boolean paused;
        Voice(NativeSoundTimeline.Cue cue,GunSoundInstance sound,Entity source) { this.cue=cue; this.sound=sound; this.source=source; }
    }
    private static final class Playback {
        List<NativeSoundTimeline.Cue> cues;
        long revision;
        final Map<String,Voice> voices=new HashMap<>();
        final HashSet<String> started=new HashSet<>();
        float previous=Float.NaN;
        boolean playing;
        Boolean firstPerson;
        Playback(Replay replay) { var channel=((TaczReplayChannels)replay.keyframes).bbstacz$sounds(); cues=NativeSoundTimeline.read(channel); revision=NativeSoundTimeline.revision(channel); }
        void stop() { voices.values().forEach(voice->voice.sound.setStop()); voices.clear(); }
    }
    public static void begin(Recorder recorder) {
        take=null;
        if (recorder==null || recorder.exception<0 || recorder.exception>=recorder.film.replays.getList().size()) return;
        var replay=recorder.film.replays.getList().get(recorder.exception);
        if (!(replay.form.get() instanceof TaczForm) && !(recorder.lastForm instanceof TaczForm)) return;
        take=new Take(recorder,replay);
        var channel=((TaczReplayChannels)recorder.keyframes).bbstacz$sounds();
        NativeSoundTimeline.append(channel,recorder.initialTick,null);
        // Cut an old cue spanning the beginning of a retake; never monitor it over the live gun.
        for (var cue:NativeSoundTimeline.read(((TaczReplayChannels)replay.keyframes).bbstacz$sounds()))
            if (cue.tick()<recorder.initialTick && cue.end()>recorder.initialTick)
                NativeSoundTimeline.append(channel,recorder.initialTick,NativeSoundTimeline.stop(cue.id()));
    }
    private static float duration(GunSoundInstance sound,float pitch) {
        ResourceLocation path=sound.getSound().getPath();
        Float seconds=LENGTHS.get(path);
        if (seconds==null) {
            try (var stream=Minecraft.getInstance().getResourceManager().getResource(path).orElseThrow().open()) {
                var wave=VorbisReader.read(new Link(path.getNamespace(),path.getPath()),stream);
                seconds=wave.data.length/(float)wave.blockAlign/wave.sampleRate;
            } catch (Exception failure) { seconds=30f; BBSTaczAddon.LOGGER.warn("Cannot determine native sound length {}",path,failure); }
            LENGTHS.put(path,seconds);
        }
        return seconds*20/Math.max(0.5f,Math.min(2,pitch));
    }
    public static void capture(Entity entity,ResourceLocation name,float volume,float pitch,int distance,boolean mono,int limit,boolean tracking,boolean relative,GunSoundInstance sound) {
        if (take==null || emitting || CommandJournal.recorder()!=take.recorder || entity!=Minecraft.getInstance().player
            || sound==null || sound.getSound()==null || !sound.canPlaySound() || FirstPersonPlayback.isRendering()) return;
        if (name==null || !Float.isFinite(volume) || !Float.isFinite(pitch) || volume<=0 || pitch<=0) return;
        String id=take.id+":"+(++take.sequence);
        NativeSoundTimeline.append(((TaczReplayChannels)take.recorder.keyframes).bbstacz$sounds(),CommandJournal.time(take.recorder),
            NativeSoundTimeline.play(id,name.toString(),volume,pitch,distance,mono,limit,tracking,relative,duration(sound,pitch)));
        take.active.put(sound,id);
    }
    public static void stopped(SoundInstance sound) {
        if (take==null) return;
        String id=take.active.remove(sound);
        if (id!=null) NativeSoundTimeline.append(((TaczReplayChannels)take.recorder.keyframes).bbstacz$sounds(),CommandJournal.time(take.recorder),NativeSoundTimeline.stop(id));
    }
    public static void finish(Recorder recorder) {
        Take captured=take; take=null;
        if (captured==null || captured.recorder!=recorder || recorder.hasNotStarted() || recorder.getTick()<=recorder.initialTick) return;
        var channel=((TaczReplayChannels)recorder.keyframes).bbstacz$sounds();
        for (String id:captured.active.values()) NativeSoundTimeline.append(channel,recorder.getTick(),NativeSoundTimeline.stop(id));
        NativeSoundTimeline.append(channel,recorder.getTick(),null);
        // Old-format gun clips remain readable, but a new take must not play both representations.
        replaceLegacyRange(recorder.film,captured.replay,recorder.initialTick,recorder.getTick());
    }
    public static void playback(BaseFilmController controller,float delta) {
        if (controller instanceof Recorder || controller.film==null || Minecraft.getInstance().player==null) return;
        boolean video=BBSModClient.getVideoRecorder().isRecording();
        boolean playing=PlaybackContext.playing(controller);
        var tracks=PLAYBACKS.computeIfAbsent(controller,key->new IdentityHashMap<>());
        // The editor may delete a replay without destroying its controller.
        var removed=tracks.entrySet().iterator();
        while(removed.hasNext()) {
            var entry=removed.next();
            if(controller.film.replays.getList().stream().noneMatch(replay->replay==entry.getKey())) { entry.getValue().stop(); removed.remove(); }
        }
        for (var replay:controller.film.replays.getList()) {
            var channel=((TaczReplayChannels)replay.keyframes).bbstacz$sounds();
            if (channel.isEmpty()) { var old=tracks.remove(replay); if(old!=null) old.stop(); continue; }
            var state=tracks.computeIfAbsent(replay,Playback::new);
            long revision=NativeSoundTimeline.revision(channel);
            if(state.revision!=revision) { state.stop(); state.started.clear(); state.cues=NativeSoundTimeline.read(channel); state.revision=revision; }
            float time=replay.getTick(controller.getTick())+FirstPersonPlayback.delta(controller,delta);
            boolean seeking=AudioCursor.seeking(state.previous,time,state.playing,playing,video);
            if (!replay.enabled.get() || CommandJournal.recorder()!=null && take!=null && take.replay==replay) { state.stop(); state.started.clear(); state.previous=time; state.playing=false; continue; }
            if (seeking) { state.stop(); state.started.clear(); }
            boolean fp=PlaybackContext.firstPerson(controller,replay);
            if(state.firstPerson!=null&&state.firstPerson!=fp) { state.stop(); state.started.clear(); }
            state.firstPerson=fp;
            var entity=controller.getEntities().get(replay.getId());
            Entity source=PlaybackContext.soundSource(controller,replay,entity,time,delta);
            if (source==null) { state.stop(); state.started.clear(); state.previous=time; state.playing=false; continue; }
            if(state.voices.values().stream().anyMatch(voice->voice.source!=source)) { state.stop(); state.started.clear(); }
            if (playing) for (var cue:state.cues) {
                if (cue.tick()>time || cue.end()<=time || state.started.contains(cue.id())) continue;
                // Already finished cues never restart on a subsequent frame. Native concurrency limits still apply.
                state.started.add(cue.id()); emitting=true;
                try {
                    var sound=NativeSoundPlayAccess.bbstacz$play(source,new ResourceLocation(cue.name()),cue.volume(),cue.pitch(),cue.distance(),cue.mono(),cue.limit(),!fp||cue.tracking(),fp&&cue.relative());
                    if (sound!=null) { state.voices.put(cue.id(),new Voice(cue,sound,source)); playedCues++; }
                } finally { emitting=false; }
            }
            var iterator=state.voices.values().iterator();
            while (iterator.hasNext()) {
                var voice=iterator.next();
                if (time>=voice.cue.end()) { voice.sound.setStop(); iterator.remove(); continue; }
                position(voice,time,!playing,video);
            }
            state.previous=time; state.playing=playing;
        }
    }
    private static void position(Voice voice,float time,boolean paused,boolean video) {
        var engine=((SoundManagerAccess)Minecraft.getInstance().getSoundManager()).bbstacz$engine();
        var handle=((SoundEngineAccess)engine).bbstacz$channels().get(voice.sound);
        if (handle==null || handle.isStopped()) return;
        boolean seek=voice.pendingSeek || video, changed=voice.paused!=paused;
        float seconds=Math.max(0,(time-voice.cue.tick())/20f*voice.cue.pitch());
        handle.execute(channel->{
            // The native buffer load/play happens asynchronously; do not lose the initial seek before it starts.
            int state=AL10.alGetSourcei(((SoundChannelAccess)channel).bbstacz$source(),AL10.AL_SOURCE_STATE);
            if (state==AL10.AL_INITIAL) return;
            // At low render rates a one-shot may finish in real time while the film is
            // still inside its cue. A paused/resumed film owns its phase, not wall time.
            boolean recover=state==AL10.AL_STOPPED && !voice.sound.getSound().shouldStream();
            if (recover) channel.play();
            if ((seek || recover) && !voice.sound.getSound().shouldStream()) AL10.alSourcef(((SoundChannelAccess)channel).bbstacz$source(),AL11.AL_SEC_OFFSET,seconds);
            if (paused) channel.pause(); else if (changed || recover) channel.unpause();
            voice.pendingSeek=false;
        });
        voice.paused=paused;
    }
    public static void pruneEditors() {
        var iterator=PLAYBACKS.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();
            if(entry.getKey() instanceof mchorse.bbs_mod.ui.film.controller.FilmEditorController && FirstPersonPlayback.editorPanel(entry.getKey())==null) {
                entry.getValue().values().forEach(Playback::stop); iterator.remove();
            }
        }
    }
    public static long playedCues() { return playedCues; }
    public static List<GunSoundInstance> voices(BaseFilmController controller) {
        var tracks=PLAYBACKS.get(controller); if(tracks==null) return List.of();
        return tracks.values().stream().flatMap(track->track.voices.values().stream()).map(voice->voice.sound).toList();
    }
    public static void stop(BaseFilmController controller) { var tracks=PLAYBACKS.remove(controller); if (tracks!=null) tracks.values().forEach(Playback::stop); }
    private static boolean nativeTrack(Replay replay) { return !((TaczReplayChannels)replay.keyframes).bbstacz$sounds().isEmpty(); }
    public static boolean hasAnyTrack(Film film) { return film.replays.getList().stream().anyMatch(RecordedAudio::nativeTrack) || film.camera.getClips(AudioClip.class).stream().anyMatch(clip->legacy(clip)); }
    public static boolean hasTrack(Film film,Replay replay) { return nativeTrack(replay) || film.camera.getClips(AudioClip.class).stream().anyMatch(clip->clip.enabled.get()&&owns(clip,replay)); }
    public static boolean suppress(Entity entity) {
        if (emitting || CommandJournal.recorder()!=null) return false;
        if (entity==Minecraft.getInstance().player) for (var controller:BBSModClient.getFilms().getControllers()) {
            var fp=controller.film.getFirstPersonReplay();
            if (controller instanceof WorldFilmController && !(controller instanceof Recorder) && !(controller instanceof mchorse.bbs_mod.film.FrozenFilmController)
                && !controller.hasFinished() && fp!=null && nativeTrack(fp)) return true;
        }
        var target=FirstPersonPlayback.target(0);
        if (entity==Minecraft.getInstance().player && target!=null && hasTrack(target.controller().film,target.replay())) return true;
        for (var entry:ReplayBindings.snapshot().entrySet()) {
            var binding=entry.getValue();
            if (!(entry.getKey() instanceof MCEntity mc) || mc.getMcEntity()!=entity || binding.replay()==null) continue;
            var controller=ReplayClock.controller(entry.getKey(),binding);
            if (controller instanceof WorldFilmController && !(controller instanceof Recorder) && hasTrack(controller.film,binding.replay())) return true;
        }
        return false;
    }
    // Compatibility only for films recorded by the old WAV implementation. No new files/clips are created.
    private static String folder(Replay replay) { return "audio/bbs_tacz/"+UUID.nameUUIDFromBytes(replay.getId().getBytes(StandardCharsets.UTF_8))+"/"; }
    private static boolean legacy(AudioClip clip) { Link link=clip.audio.get(); return link!=null&&link.source.equals(Link.ASSETS)&&link.path.startsWith("audio/bbs_tacz/"); }
    private static boolean owns(AudioClip clip,Replay replay) { return legacy(clip)&&clip.audio.get().path.startsWith(folder(replay)); }
    public static boolean muteTake(mchorse.bbs_mod.utils.clips.ClipContext context,AudioClip clip) { return take!=null&&take.recorder.getContext()==context&&owns(clip,take.replay); }
    private static void replaceLegacyRange(Film film,Replay replay,int start,int end) {
        for (var clip:film.camera.getClips(AudioClip.class)) {
            int left=clip.tick.get(); long right=(long)left+clip.duration.get(); if (!owns(clip,replay)||left>=end||right<=start) continue;
            film.camera.remove(clip);
            if (left<start) { var head=(AudioClip)clip.copy(); head.duration.set(start-left); film.camera.addClip(head); }
            if (right>end) { var tail=(AudioClip)clip.breakDown(end-left); tail.tick.set(end); film.camera.addClip(tail); }
        }
    }
    public static void clear() { take=null; PLAYBACKS.values().forEach(tracks->tracks.values().forEach(Playback::stop)); PLAYBACKS.clear(); LENGTHS.clear(); emitting=false; }
}
