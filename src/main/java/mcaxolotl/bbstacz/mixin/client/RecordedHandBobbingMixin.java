package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.api.client.event.RenderItemInHandBobEvent;
import com.tacz.guns.client.event.FirstPersonRenderGunEvent;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native TACZ view bob cancellation also applies when a film gun is shown over an empty live hand. */
@Mixin(value = FirstPersonRenderGunEvent.class, remap = false)
public abstract class RecordedHandBobbingMixin {
    @Inject(method = "cancelItemInHandViewBobbing", at = @At("HEAD"))
    private static void bbstacz$recorded(RenderItemInHandBobEvent.BobView event, CallbackInfo ci) {
        if (FirstPersonPlayback.target(Minecraft.getInstance().getFrameTime()) != null) event.setCanceled(true);
    }
}
