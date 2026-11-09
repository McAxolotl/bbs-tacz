package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.client.F6Starts;
import mcaxolotl.bbstacz.client.F6FrameGate;
import mcaxolotl.bbstacz.network.F6ServerStart;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.film.WorldFilmController;
import mchorse.bbs_mod.actions.ActionState;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.film.WorldVideoExportSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** F6 starts a fresh playback, not the editor's same-id frozen frame or server-side preview. */
@Mixin(value = WorldVideoExportSession.class, remap = false)
public abstract class F6PlaybackMixin {
    @Shadow private Film film;
    @Shadow private boolean firstTickPaused;
    @ModifyArg(method = "start", at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/film/WorldVideoExportSession;begin(IIIJ)Z"), index = 3)
    private long bbstacz$startupDelay(long delay) {
        var replay = film == null ? null : film.getFirstPersonReplay();
        // Even with no cosmetic delay, asynchronous playback has to reach the warm-up state machine.
        return replay != null && replay.form.get() instanceof TaczForm ? Math.max(1, delay) : delay;
    }
    @Unique private String bbstacz$pendingFilm;
    @Unique private F6Starts.Ticket bbstacz$ticket;
    @Unique private long bbstacz$requestedAt;
    @Inject(method = "start", at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/film/Films;playFilm(Ljava/lang/String;Z)V"))
    private void bbstacz$unfreeze(String filmId, Film film, CallbackInfoReturnable<Boolean> ci) {
        var replay = film == null ? null : film.getFirstPersonReplay();
        if (filmId == null || replay == null || !(replay.form.get() instanceof TaczForm)) return;
        bbstacz$pendingFilm = filmId; bbstacz$requestedAt = System.currentTimeMillis(); bbstacz$ticket = F6Starts.request(filmId);
        var films = BBSModClient.getFilms();
        films.unfreeze(filmId);
        while (films.getController(filmId) != null) Films.stopFilm(filmId);
        // Ordered native packets: STOP removes FILM_EDITOR/old playback before the toggle starts NORMAL.
        if (ClientNetwork.isIsBBSModOnServer()) ClientNetwork.sendActionState(filmId, ActionState.STOP, 0);
    }

    @Redirect(method = "start", at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/film/Films;playFilm(Ljava/lang/String;Z)V"))
    private void bbstacz$serverHold(String id, boolean camera) {
        if (bbstacz$pendingFilm != null && ClientNetwork.isIsBBSModOnServer()) {
            // The editor's latest FP flag/form/channels may not be on disk yet. The native
            // chunked SAVE packets precede START on the same connection/server task queue.
            // Keep BBS's repository permission checks instead of adding an unbounded payload.
            ContentType.FILMS.getRepository().save(id,film.toData().asMap());
            var buffer=PacketByteBufs.create(); buffer.writeUtf(id); ClientPlayNetworking.send(F6ServerStart.ID,buffer);
        } else Films.playFilm(id,camera);
    }

    @Inject(method = "shouldAbortWarmup", at = @At("HEAD"), cancellable = true)
    private void bbstacz$awaitPlayback(CallbackInfoReturnable<Boolean> ci) {
        // The server sends a serialized film asynchronously; no controller yet is not a failed playback.
        if (bbstacz$pendingFilm != null && !BBSModClient.getFilms().has(bbstacz$pendingFilm)
            && System.currentTimeMillis() - bbstacz$requestedAt < 30_000) ci.setReturnValue(false);
    }

    @Inject(method = "isWarmupReady", at = @At("HEAD"), cancellable = true)
    private void bbstacz$worldClock(CallbackInfoReturnable<Boolean> ci) {
        if (bbstacz$pendingFilm == null) return;
        var controller=BBSModClient.getFilms().getController(bbstacz$pendingFilm);
        if (!(controller instanceof WorldFilmController world)) { ci.setReturnValue(false); return; }
        world.paused=true;
        firstTickPaused=true;
        ci.setReturnValue(true);
    }

    @Inject(method = "onRecordingStarted", at = @At("HEAD"), cancellable = true)
    private void bbstacz$frameZero(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (bbstacz$pendingFilm == null) return;
        var controller=BBSModClient.getFilms().getController(bbstacz$pendingFilm);
        if (!(controller instanceof WorldFilmController world)) return;
        String id=bbstacz$pendingFilm;
        world.paused=true;
        F6FrameGate.await(bbstacz$ticket, () -> {
            if (!BBSModClient.getVideoRecorder().isRecording() || BBSModClient.getFilms().getController(id) != world) return;
            world.paused=false;
            if (ClientNetwork.isIsBBSModOnServer()) ClientNetwork.sendActionState(id,ActionState.PLAY,world.getTick());
        });
        ci.cancel();
    }

    @Inject(method = "isFinished", at = @At("HEAD"), cancellable = true)
    private void bbstacz$failedSink(CallbackInfoReturnable<Boolean> ci) {
        if (bbstacz$pendingFilm != null && !BBSModClient.getVideoRecorder().isRecording()) {
            ((WorldVideoExportSession)(Object)this).cancel(); ci.setReturnValue(false);
        }
    }

    @Inject(method = "teardown", at = @At("HEAD"))
    private void bbstacz$cancel(boolean cancelled, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (bbstacz$pendingFilm == null || !cancelled && !F6FrameGate.waiting(bbstacz$ticket)) return;
        F6Starts.cancel(bbstacz$ticket);
        var films = BBSModClient.getFilms(); films.unfreeze(bbstacz$pendingFilm);
        while (films.getController(bbstacz$pendingFilm) != null) Films.stopFilm(bbstacz$pendingFilm);
        // Idempotent stop, never the toggle which could restart a film already stopped by this cancellation.
        if (ClientNetwork.isIsBBSModOnServer()) ClientNetwork.sendActionState(bbstacz$pendingFilm, ActionState.STOP, 0);
    }

    @Inject(method = "teardown", at = @At("TAIL"))
    private void bbstacz$clear(boolean cancelled, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        F6FrameGate.cancel(bbstacz$ticket);
        bbstacz$pendingFilm = null; bbstacz$requestedAt = 0; bbstacz$ticket = null;
    }
}
