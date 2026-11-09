package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.client.F6FrameGate;
import mchorse.bbs_mod.utils.VideoRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VideoRecorder.class, remap = false)
public abstract class F6FirstFrameMixin {
    @Inject(method = "recordFrame", at = @At("TAIL"))
    private void bbstacz$captured(CallbackInfo ci) {
        var recorder=(VideoRecorder)(Object)this;
        if(recorder.isRecording() && recorder.getCounter()>0) F6FrameGate.captured();
    }
}
