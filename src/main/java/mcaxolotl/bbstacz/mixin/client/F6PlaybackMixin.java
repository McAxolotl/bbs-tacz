package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.forms.TaczForm;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.film.WorldFilmController;
import mchorse.bbs_mod.actions.ActionState;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.film.WorldVideoExportSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** F6 starts a fresh playback, not the editor's same-id frozen frame or server-side preview. */
@Mixin(value = WorldVideoExportSession.class, remap = false)
public abstract class F6PlaybackMixin {
    @Unique private String bbstacz$pendingFilm;
    @Unique private long bbstacz$requestedAt;
    @Inject(method = "start", at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/film/Films;playFilm(Ljava/lang/String;Z)V"))
    private void bbstacz$unfreeze(String filmId, Film film, CallbackInfoReturnable<Boolean> ci) {
        var replay = film == null ? null : film.getFirstPersonReplay();
        if (filmId == null || replay == null || !(replay.form.get() instanceof TaczForm)) return;
        bbstacz$pendingFilm = filmId; bbstacz$requestedAt = System.currentTimeMillis();
        var films = BBSModClient.getFilms();
        films.unfreeze(filmId);
        while (films.getController(filmId) != null) Films.stopFilm(filmId);
        // Ordered native packets: STOP removes FILM_EDITOR/old playback before the toggle starts NORMAL.
        if (ClientNetwork.isIsBBSModOnServer()) ClientNetwork.sendActionState(filmId, ActionState.STOP, 0);
    }

    @Inject(method = "shouldAbortWarmup", at = @At("HEAD"), cancellable = true)
    private void bbstacz$awaitPlayback(CallbackInfoReturnable<Boolean> ci) {
        // The server sends a serialized film asynchronously; no controller yet is not a failed playback.
        if (bbstacz$pendingFilm != null && !BBSModClient.getFilms().has(bbstacz$pendingFilm)
            && System.currentTimeMillis() - bbstacz$requestedAt < 30_000) ci.setReturnValue(false);
    }

    @Inject(method = "isWarmupReady", at = @At("HEAD"), cancellable = true)
    private void bbstacz$worldClock(CallbackInfoReturnable<Boolean> ci) {
        if (bbstacz$pendingFilm != null && !(BBSModClient.getFilms().getController(bbstacz$pendingFilm) instanceof WorldFilmController)) ci.setReturnValue(false);
    }

    @Inject(method = "teardown", at = @At("TAIL"))
    private void bbstacz$clear(boolean cancelled, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        bbstacz$pendingFilm = null; bbstacz$requestedAt = 0;
    }
}
