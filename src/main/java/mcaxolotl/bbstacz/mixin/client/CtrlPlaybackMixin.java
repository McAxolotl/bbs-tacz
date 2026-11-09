package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.client.CtrlPlaybackStarts;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.network.F6ServerStart;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.actions.ActionState;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.film.FrozenFilmController;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=BBSModClient.class,remap=false)
public abstract class CtrlPlaybackMixin {
    @Inject(method="keyPlayFilm",at=@At("HEAD"),cancellable=true)
    private void bbstacz$atomicPlay(CallbackInfo ci) {
        if(BBSModClient.getDashboardIfCreated()==null || !ClientNetwork.isIsBBSModOnServer()) return;
        var panel=BBSModClient.getDashboard().getPanel(UIFilmPanel.class); var film=panel.getData();
        var fp=film==null?null:film.getFirstPersonReplay(); if(fp==null||!(fp.form.get() instanceof TaczForm)) return;
        String id=film.getId(); mcaxolotl.bbstacz.BBSTaczAddon.LOGGER.debug("Ctrl start {}",id); var films=BBSModClient.getFilms(); var controller=films.getController(id);
        if(controller!=null && !(controller instanceof FrozenFilmController)) { CtrlPlaybackStarts.cancel(id); Films.playFilm(id,false); ci.cancel(); return; }
        if(CtrlPlaybackStarts.pending(id)) { CtrlPlaybackStarts.cancel(id); ClientNetwork.sendActionState(id,ActionState.STOP,0); ci.cancel(); return; }
        films.unfreeze(id); ClientNetwork.sendActionState(id,ActionState.STOP,0);
        CtrlPlaybackStarts.request(id); ContentType.FILMS.getRepository().save(id,film.toData().asMap());
        var packet=PacketByteBufs.create(); packet.writeUtf(id); packet.writeBoolean(true); ClientPlayNetworking.send(F6ServerStart.ID,packet); ci.cancel();
    }
}
