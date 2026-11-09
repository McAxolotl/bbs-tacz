package mcaxolotl.bbstacz.mixin;

import java.util.List;
import mcaxolotl.bbstacz.recording.CallbackList;
import mchorse.bbs_mod.actions.ActionManager;
import mchorse.bbs_mod.actions.ActionPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native tick/stop callbacks can stop or replace players while the registry is being traversed. */
@Mixin(value = ActionManager.class, remap = false)
public abstract class ActionManagerListMixin {
    @Shadow private List<ActionPlayer> players;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void bbstacz$safeLifecycleIteration(CallbackInfo ci) {
        players = new CallbackList<>(players);
    }
}
