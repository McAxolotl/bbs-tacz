package mcaxolotl.bbstacz.mixin;

import mcaxolotl.bbstacz.recording.FrameFactory;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.ReplayBindings;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(value = ReplayKeyframes.class, remap = false)
public abstract class ReplayKeyframesMixin extends ValueGroup implements TaczReplayChannels {
    @Unique private KeyframeChannel<FrameSnapshot> bbstacz$state;
    @Unique private KeyframeChannel<String> bbstacz$commands;
    protected ReplayKeyframesMixin(String id) { super(id); }
    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbstacz$register(String id, CallbackInfo ci) {
        bbstacz$state = new KeyframeChannel<>(STATE, FrameFactory.INSTANCE);
        bbstacz$commands = new KeyframeChannel<>(COMMANDS, KeyframeFactories.STRING);
        bbstacz$state.synced(); bbstacz$commands.synced();
        add(bbstacz$state); add(bbstacz$commands);
    }
    @Override public KeyframeChannel<FrameSnapshot> bbstacz$state() { return bbstacz$state; }
    @Override public KeyframeChannel<String> bbstacz$commands() { return bbstacz$commands; }
    @Inject(method = "apply(FLmchorse/bbs_mod/forms/entities/IEntity;Ljava/util/List;)V", at = @At("TAIL"))
    private void bbstacz$apply(float tick, IEntity entity, List<String> groups, CallbackInfo ci) {
        if (groups == null || groups.isEmpty()) ReplayBindings.bind(entity, bbstacz$state, tick);
    }
}
