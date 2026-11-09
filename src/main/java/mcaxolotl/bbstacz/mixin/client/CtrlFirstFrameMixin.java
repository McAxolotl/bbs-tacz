package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.client.CtrlPlaybackStarts;
import mchorse.bbs_mod.film.WorldFilmController;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=WorldFilmController.class,remap=false)
public abstract class CtrlFirstFrameMixin {
    @Inject(method="render",at=@At("RETURN"))
    private void bbstacz$openingFrame(WorldRenderContext context,CallbackInfo ci) { CtrlPlaybackStarts.rendered((WorldFilmController)(Object)this); }
}
