package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.client.F6Starts;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Films;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Films.class, remap = false)
public abstract class F6PlayResponseMixin {
    @Inject(method = "playFilm(Lmchorse/bbs_mod/film/Film;Z)V", at = @At("HEAD"), cancellable = true)
    private static void bbstacz$cancelled(Film film, boolean camera, CallbackInfo ci) {
        if (film != null && F6Starts.receive(film.getId())) ci.cancel();
    }
}
