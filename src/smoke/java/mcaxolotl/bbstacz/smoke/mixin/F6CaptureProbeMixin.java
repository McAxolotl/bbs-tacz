package mcaxolotl.bbstacz.smoke.mixin;

import mcaxolotl.bbstacz.smoke.ReviewProbe;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.utils.VideoRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VideoRecorder.class, remap = false)
public abstract class F6CaptureProbeMixin {
    @Inject(method = "recordFrame", at = @At("HEAD"), cancellable = true)
    private void bbstacz$first(CallbackInfo ci) {
        var recorder=(VideoRecorder)(Object)this;
        if(!recorder.isRecording()||recorder.getCounter()!=0) return;
        if(ReviewProbe.failFirstFrame) { ReviewProbe.failFirstFrame=false; recorder.stopRecording(false); ci.cancel(); return; }
        if(ReviewProbe.exportFilm==null) return;
        var controller=BBSModClient.getFilms().getController(ReviewProbe.exportFilm);
        if(controller!=null) ReviewProbe.firstExportTick=controller.getTick();
    }
}
