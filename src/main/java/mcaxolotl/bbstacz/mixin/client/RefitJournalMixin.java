package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.screen.RefitTransform;
import mcaxolotl.bbstacz.client.CommandJournal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RefitTransform.class, remap = false)
public abstract class RefitJournalMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private static void bbstacz$open(CallbackInfo ci) { CommandJournal.capture("refit_open"); }
    @Inject(method = "changeRefitScreenView", at = @At("RETURN"))
    private static void bbstacz$view(AttachmentType attachment, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) CommandJournal.capture("refit_view", attachment.name());
    }
}
