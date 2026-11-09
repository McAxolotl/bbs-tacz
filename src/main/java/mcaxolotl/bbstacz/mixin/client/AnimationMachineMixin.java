package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import mcaxolotl.bbstacz.client.CommandJournal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnimationStateMachine.class, remap = false)
public abstract class AnimationMachineMixin {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void bbstacz$trigger(String condition, CallbackInfo ci) {
        if (CommandJournal.recording((AnimationStateMachine<?>) (Object) this)) CommandJournal.capture("trigger", condition);
    }
}
