package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.client.RecordedAudio;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Films;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Films.class, remap = false)
public abstract class AudioTakeMixin {
    @Inject(method = "startRecording(Lmchorse/bbs_mod/film/Film;IIZ)V", at = @At("RETURN"))
    private void bbstacz$start(Film film, int replay, int tick, boolean mark, CallbackInfo ci) { RecordedAudio.begin(BBSModClient.getFilms().getRecorder()); }
    @Inject(method = "stopRecording", at = @At("HEAD"))
    private void bbstacz$stop(CallbackInfoReturnable<mchorse.bbs_mod.film.Recorder> ci) { RecordedAudio.finish(BBSModClient.getFilms().getRecorder()); }
}
