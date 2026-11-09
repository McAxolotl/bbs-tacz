package mcaxolotl.bbstacz.smoke;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import net.minecraft.client.Minecraft;

/** A just-enabled FP flag in the editor must not be replaced by the older persisted third-person film. */
public final class F6DraftExportReview extends F6ExportReview {
    @Override protected Film exportFilm(Film film) {
        film.replays.getList().get(0).fp.set(false); return film;
    }
    @Override protected void beforeF6(Film film,UIFilmPanel panel,Minecraft client) throws Exception {
        boolean old=client.getSingleplayerServer().submit(()->BBSMod.getFilms().load(film.getId()).getFirstPersonReplay()==null).get();
        ReviewClient.require(old,"Reproduction setup did not preserve the older third-person repository copy");
        film.replays.getList().get(0).fp.set(true);
        System.out.println("[REVIEW F6 draft] editor FP=true; persisted FP=false; no manual save after the edit");
    }
}
