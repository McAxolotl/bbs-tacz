package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.client.RecordedAudio;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public abstract class NativeSoundStopMixin {
    @Inject(method = "stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"))
    private void bbstacz$stop(SoundInstance sound, CallbackInfo ci) { RecordedAudio.stopped(sound); }
}
