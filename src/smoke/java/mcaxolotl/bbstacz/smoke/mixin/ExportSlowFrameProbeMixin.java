package mcaxolotl.bbstacz.smoke.mixin;

import mcaxolotl.bbstacz.smoke.ReviewProbe;
import mchorse.bbs_mod.BBSModClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-only GPU-stall surrogate. Never included in the distributable mod. */
@Mixin(GameRenderer.class)
public abstract class ExportSlowFrameProbeMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void bbstacz$slowFrame(float delta,long time,boolean level,CallbackInfo ci) {
        if(ReviewProbe.exportSleepMillis>0 && BBSModClient.getVideoRecorder().isRecording())
            try { Thread.sleep(ReviewProbe.exportSleepMillis); } catch(InterruptedException failure) { Thread.currentThread().interrupt(); }
    }
}
