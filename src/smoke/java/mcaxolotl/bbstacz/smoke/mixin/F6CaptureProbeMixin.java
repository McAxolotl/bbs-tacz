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
        if(!recorder.isRecording()) return;
        ReviewProbe.clock();
        if (recorder.getCounter()%150==0 && ReviewProbe.exportFilm!=null) {
            var current=BBSModClient.getFilms().getController(ReviewProbe.exportFilm);
            System.out.println("[REVIEW export frame] frames="+recorder.getCounter()+" film_tick="+(current==null?-1:current.getTick())
                +" paused="+(current==null?null:current.paused)+" mc_paused="+net.minecraft.client.Minecraft.getInstance().isPaused()
                +" server_ticks="+recorder.serverTicks+" fraction="+net.minecraft.client.Minecraft.getInstance().getFrameTime()
                +" texture="+recorder.getTextureId()+" current_texture="+mchorse.bbs_mod.client.BBSRendering.getTexture().id
                +" framebuffer="+mchorse.bbs_mod.client.BBSRendering.getFramebuffer().width+"x"+mchorse.bbs_mod.client.BBSRendering.getFramebuffer().height);
        }
        if(recorder.getCounter()!=0) return;
        if(ReviewProbe.failFirstFrame) { ReviewProbe.failFirstFrame=false; recorder.stopRecording(false); ci.cancel(); return; }
        if(ReviewProbe.exportFilm==null) return;
        var controller=BBSModClient.getFilms().getController(ReviewProbe.exportFilm);
        if(controller!=null) ReviewProbe.firstExportTick=controller.getTick();
    }
}
