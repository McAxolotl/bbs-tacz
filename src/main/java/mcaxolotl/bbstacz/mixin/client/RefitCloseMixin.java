package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.client.gui.GunRefitScreen;
import mcaxolotl.bbstacz.client.CommandJournal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GunRefitScreen.class)
public abstract class RefitCloseMixin {
    @Inject(method = "onClose", at = @At("HEAD"))
    private void bbstacz$close(CallbackInfo ci) { CommandJournal.capture("refit_close"); }
}
