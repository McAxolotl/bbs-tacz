package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.compat.playeranimator.animation.AnimationManager;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import mcaxolotl.bbstacz.client.ActorAnimations;
import mcaxolotl.bbstacz.client.CommandJournal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Accepted gun events must replace an old TP one-shot, rather than silently retaining reload/fire. */
@Mixin(value=AnimationManager.class,remap=false)
public abstract class ThirdPersonOneShotMixin {
    @Redirect(method="lambda$playOnceAnimation$3",at=@At(value="INVOKE",target="Ldev/kosmx/playerAnim/api/layered/IAnimation;isActive()Z"))
    private static boolean bbstacz$allowAcceptedInterrupt(IAnimation current,AbstractClientPlayer player,ResourceLocation layer,dev.kosmx.playerAnim.core.data.KeyframeAnimation animation) {
        return !ActorAnimations.recordsNativeEvents(player)&&current.isActive();
    }
    @Inject(method="playOnceAnimation",at=@At("TAIL"))
    private static void bbstacz$recordNativeVariant(AbstractClientPlayer player,GunDisplayInstance display,ResourceLocation layer,String animation,CallbackInfo ci) {
        if(player==Minecraft.getInstance().player&&ActorAnimations.recordsNativeEvents(player))
            CommandJournal.capture("tp_animation",layer.toString(),display.getPlayerAnimator3rd()==null?"":display.getPlayerAnimator3rd().toString(),animation);
    }
}
