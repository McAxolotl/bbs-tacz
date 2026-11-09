package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.client.NativeCapturedSound;
import mchorse.bbs_mod.audio.MinecraftSoundCapture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value=MinecraftSoundCapture.CapturedSound.class,remap=false)
public abstract class NativeCapturedSoundMixin implements NativeCapturedSound {
    @Unique private boolean bbstacz$native;
    @Override public boolean bbstacz$isNativeGun() { return bbstacz$native; }
    @Override public void bbstacz$nativeGun(boolean nativeGun) { bbstacz$native=nativeGun; }
}
