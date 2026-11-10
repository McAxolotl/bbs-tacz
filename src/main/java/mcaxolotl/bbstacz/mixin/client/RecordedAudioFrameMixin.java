package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.client.RecordedAudio;
import mchorse.bbs_mod.film.BaseFilmController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Both world and editor viewports prepare actor poses here, including fractional/editor-only playback. */
@Mixin(value=BaseFilmController.class,remap=false)
public abstract class RecordedAudioFrameMixin {
    @Inject(method="startRenderFrame",at=@At("TAIL"))
    private void bbstacz$nativeAudio(float delta,CallbackInfo ci) { RecordedAudio.playback((BaseFilmController)(Object)this,delta); }
}
