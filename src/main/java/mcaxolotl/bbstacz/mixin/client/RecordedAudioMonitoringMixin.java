package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.client.RecordedAudio;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.misc.AudioClientClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.utils.clips.ClipContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep background tracks, but never monitor the previous gun take over the replay currently being recorded. */
@Mixin(value = AudioClientClip.class, remap = false)
public abstract class RecordedAudioMonitoringMixin {
    @Inject(method = "applyClip", at = @At("HEAD"), cancellable = true)
    private void bbstacz$overdub(ClipContext context, Position position, CallbackInfo ci) {
        var clip = (AudioClientClip)(Object)this;
        if (RecordedAudio.muteTake(context, clip)) { BBSModClient.getSounds().stopOwned(clip); ci.cancel(); }
    }
}
