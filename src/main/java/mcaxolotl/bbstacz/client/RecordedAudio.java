package mcaxolotl.bbstacz.client;

import com.tacz.guns.client.sound.GunSoundInstance;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import mcaxolotl.bbstacz.BBSTaczAddon;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.Pcm16;
import mcaxolotl.bbstacz.recording.ReplayBindings;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.audio.Wave;
import mchorse.bbs_mod.audio.ogg.VorbisReader;
import mchorse.bbs_mod.audio.wav.WaveWriter;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
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

/** Capture actual audible native cues, then commit ordinary BBS audio clips when the take is stopped. */
public final class RecordedAudio {
    private static Take take;
    private RecordedAudio() {}
    private static final class Cue {
        final ResourceLocation path;
        final String label;
        final float tick, volume, pitch;
        float stopped = Float.POSITIVE_INFINITY;
        Cue(GunSoundInstance sound, float tick) {
            path = sound.getSound().getPath(); label = String.valueOf(sound.getRegistryName()); this.tick = tick;
            volume = Math.max(0, Math.min(1, sound.getVolume())); pitch = Math.max(0.5f, Math.min(2, sound.getPitch()));
        }
    }
    private static final class Take {
        final Recorder recorder;
        final Replay replay;
        final ArrayList<Cue> cues = new ArrayList<>();
        final Map<SoundInstance, Cue> active = new IdentityHashMap<>();
        Take(Recorder recorder, Replay replay) { this.recorder = recorder; this.replay = replay; }
    }
    public static void begin(Recorder recorder) {
        take = null;
        if (recorder == null || recorder.exception < 0 || recorder.exception >= recorder.film.replays.getList().size()) return;
        var replay = recorder.film.replays.getList().get(recorder.exception);
        if (replay.form.get() instanceof TaczForm || recorder.lastForm instanceof TaczForm) take = new Take(recorder, replay);
    }
    public static void capture(Entity entity, GunSoundInstance sound) {
        if (take == null || CommandJournal.recorder() != take.recorder || entity != Minecraft.getInstance().player
            || sound == null || sound.getSound() == null || !sound.canPlaySound() || FirstPersonPlayback.isRendering()) return;
        Cue cue = new Cue(sound, CommandJournal.time(take.recorder));
        if (!Float.isFinite(cue.volume) || !Float.isFinite(cue.pitch) || cue.volume <= 0) return;
        take.cues.add(cue); take.active.put(sound, cue);
    }
    public static void stopped(SoundInstance sound) {
        if (take == null) return;
        Cue cue = take.active.remove(sound);
        if (cue != null) cue.stopped = Math.max(cue.tick, CommandJournal.time(take.recorder));
    }
    public static void finish(Recorder recorder) {
        Take captured = take; take = null;
        if (captured == null || captured.recorder != recorder || recorder.hasNotStarted() || recorder.getTick() <= recorder.initialTick) return;
        replaceRange(recorder.film, captured.replay, recorder.initialTick, recorder.getTick());
        var cache = new HashMap<ResourceLocation, Wave>();
        for (Cue cue : captured.cues) {
            try {
                Wave source = cache.get(cue.path);
                if (source == null) {
                    var resource = Minecraft.getInstance().getResourceManager().getResource(cue.path).orElseThrow();
                    try (var stream = resource.open()) { source = VorbisReader.read(new Link(cue.path.getNamespace(), cue.path.getPath()), stream); }
                    cache.put(cue.path, source);
                }
                int tick = (int)Math.floor(cue.tick);
                byte[] pcm = Pcm16.bake(source.data, source.numChannels, source.sampleRate, cue.pitch, (cue.tick - tick) / 20d,
                    Math.max(0, (Math.min(cue.stopped, recorder.getTick()) - cue.tick) / 20d));
                if (pcm.length == 0) continue;
                var wave = new Wave(1, source.numChannels, source.sampleRate, 16, pcm);
                Link link = store(captured.replay, wave);
                var clip = (AudioClip)recorder.film.camera.getFactory().create(Link.bbs("audio"));
                clip.title.set("TACZ: " + cue.label); clip.audio.set(link); clip.tick.set(tick);
                clip.duration.set(Math.max(1, Math.min(recorder.getTick() - tick, (int)Math.ceil(pcm.length / (double)wave.blockAlign / wave.sampleRate * 20d)))); clip.volume.set(cue.volume);
                clip.layer.set(recorder.film.camera.findFreeLayer(clip)); recorder.film.camera.addClip(clip);
            } catch (Exception failure) { BBSTaczAddon.LOGGER.warn("Cannot bake recorded TACZ sound {}", cue.path, failure); }
        }
    }
    private static String folder(Replay replay) {
        return "audio/bbs_tacz/" + UUID.nameUUIDFromBytes(replay.getId().getBytes(StandardCharsets.UTF_8)) + "/";
    }
    private static Link store(Replay replay, Wave wave) throws Exception {
        var hash = MessageDigest.getInstance("SHA-256");
        hash.update(ByteBuffer.allocate(8).putInt(wave.numChannels).putInt(wave.sampleRate).array()); hash.update(wave.data);
        String name = HexFormat.of().formatHex(hash.digest()) + ".wav";
        Path folder = BBSMod.getAssetsPath(folder(replay)).toPath(); Files.createDirectories(folder);
        Path file = folder.resolve(name);
        if (!Files.isRegularFile(file) || Files.size(file) != 44L + wave.data.length) {
            Path temporary = Files.createTempFile(folder, "bake-", ".tmp");
            try {
                WaveWriter.write(temporary.toFile(), wave);
                try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (java.nio.file.AtomicMoveNotSupportedException unsupported) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temporary); }
        }
        return Link.assets(folder(replay) + name);
    }
    private static boolean owns(AudioClip clip, Replay replay) {
        Link link = clip.audio.get();
        return link != null && link.source.equals(Link.ASSETS) && link.path.startsWith(folder(replay));
    }
    public static boolean muteTake(mchorse.bbs_mod.utils.clips.ClipContext context, AudioClip clip) {
        return take != null && take.recorder.getContext() == context && owns(clip, take.replay);
    }
    public static boolean hasAnyTrack(Film film) {
        return film.camera.getClips(AudioClip.class).stream().anyMatch(clip -> {
            Link link = clip.audio.get(); return link != null && link.source.equals(Link.ASSETS) && link.path.startsWith("audio/bbs_tacz/");
        });
    }
    public static boolean hasTrack(Film film, Replay replay) {
        return film.camera.getClips(AudioClip.class).stream().anyMatch(clip -> clip.enabled.get() && owns(clip, replay));
    }
    private static void replaceRange(Film film, Replay replay, int start, int end) {
        for (var clip : film.camera.getClips(AudioClip.class)) {
            int left = clip.tick.get(); long right = (long)left + clip.duration.get();
            if (!owns(clip, replay) || left >= end || right <= start) continue;
            film.camera.remove(clip);
            if (left < start) { var head = (AudioClip)clip.copy(); head.duration.set(start - left); film.camera.addClip(head); }
            if (right > end) { var tail = (AudioClip)clip.breakDown(end - left); tail.tick.set(end); film.camera.addClip(tail); }
        }
    }
    public static boolean suppress(Entity entity) {
        if (CommandJournal.recorder() != null) return false;
        var target = FirstPersonPlayback.target(0);
        if (entity == Minecraft.getInstance().player && target != null && hasTrack(target.controller().film, target.replay())) return true;
        // Server fire notifications still drive gameplay/other clients; only their duplicate audible cue is suppressed here.
        for (var entry : ReplayBindings.snapshot().entrySet()) {
            var binding = entry.getValue();
            if (!(entry.getKey() instanceof MCEntity mc) || mc.getMcEntity() != entity || binding.replay() == null) continue;
            var controller = ReplayClock.controller(entry.getKey(), binding);
            if (controller instanceof WorldFilmController && !(controller instanceof Recorder) && hasTrack(controller.film, binding.replay())) return true;
        }
        return false;
    }
    public static void clear() { take = null; }
}
