package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.compat.playeranimator.animation.AnimationManager;
import mcaxolotl.bbstacz.client.forms.TaczVisualPlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Visual proxies consume the session's evaluated pose, not a second independent clock/state machine. */
@Mixin(value = AnimationManager.class, remap = false)
public abstract class VisualPlayerAnimatorMixin {
    @Inject(method = "updateAnimation", at = @At("HEAD"), cancellable = true)
    private static void bbstacz$session(AbstractClientPlayer player, CallbackInfo ci) { if (player instanceof TaczVisualPlayer) ci.cancel(); }
}
