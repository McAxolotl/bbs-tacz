package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.api.client.animation.statemachine.AnimationStateContext;
import mcaxolotl.bbstacz.client.CommandJournal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnimationStateContext.class, remap = false)
public abstract class AnimationCommandsMixin {
    @Inject(method = "runAnimation", at = @At("HEAD"))
    private void bbstacz$runAnimation(String name, int track, boolean blending, int playType, float transitionTime, CallbackInfo ci) {
        if (CommandJournal.recording(((AnimationStateContext) (Object) this).getStateMachine())) CommandJournal.capture("runAnimation", name, track, blending, playType, transitionTime);
    }
    @Inject(method = "stopAnimation", at = @At("HEAD"))
    private void bbstacz$stopAnimation(int track, CallbackInfo ci) {
        if (CommandJournal.recording(((AnimationStateContext) (Object) this).getStateMachine())) CommandJournal.capture("stopAnimation", track);
    }
    @Inject(method = "holdAnimation", at = @At("HEAD"))
    private void bbstacz$holdAnimation(int track, CallbackInfo ci) {
        if (CommandJournal.recording(((AnimationStateContext) (Object) this).getStateMachine())) CommandJournal.capture("holdAnimation", track);
    }
    @Inject(method = "pauseAnimation", at = @At("HEAD"))
    private void bbstacz$pauseAnimation(int track, CallbackInfo ci) {
        if (CommandJournal.recording(((AnimationStateContext) (Object) this).getStateMachine())) CommandJournal.capture("pauseAnimation", track);
    }
    @Inject(method = "resumeAnimation", at = @At("HEAD"))
    private void bbstacz$resumeAnimation(int track, CallbackInfo ci) {
        if (CommandJournal.recording(((AnimationStateContext) (Object) this).getStateMachine())) CommandJournal.capture("resumeAnimation", track);
    }
    @Inject(method = "setAnimationProgress", at = @At("HEAD"))
    private void bbstacz$setAnimationProgress(int track, float progress, boolean normalization, CallbackInfo ci) {
        if (CommandJournal.recording(((AnimationStateContext) (Object) this).getStateMachine())) CommandJournal.capture("setAnimationProgress", track, progress, normalization);
    }
    @Inject(method = "adjustAnimationProgress", at = @At("HEAD"))
    private void bbstacz$adjustAnimationProgress(int track, float progress, boolean normalization, CallbackInfo ci) {
        if (CommandJournal.recording(((AnimationStateContext) (Object) this).getStateMachine())) CommandJournal.capture("adjustAnimationProgress", track, progress, normalization);
    }
}
