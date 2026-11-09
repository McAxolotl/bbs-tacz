package mcaxolotl.bbstacz.mixin.client;

import java.util.List;
import mcaxolotl.bbstacz.client.LiveCapture;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.entities.IEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ReplayKeyframes.class, remap = false)
public abstract class RecordTickMixin {
    @Inject(method = "record", at = @At("TAIL"))
    private void bbstacz$record(int tick, IEntity entity, List<String> groups, CallbackInfo ci) {
        if (groups == null || groups.isEmpty()) LiveCapture.captureTick((TaczReplayChannels) this, tick, entity);
    }
}
