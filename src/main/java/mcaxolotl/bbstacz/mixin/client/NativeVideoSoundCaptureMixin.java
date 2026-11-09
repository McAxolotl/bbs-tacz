package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.client.sound.GunSoundInstance;
import mcaxolotl.bbstacz.client.NativeVideoSounds;
import mchorse.bbs_mod.audio.MinecraftSoundCapture;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=MinecraftSoundCapture.class,remap=false)
public abstract class NativeVideoSoundCaptureMixin {
    @Unique private int bbstacz$before;
    @Inject(method="begin",at=@At("TAIL"))
    private void bbstacz$begin(CallbackInfo ci) { NativeVideoSounds.begin((MinecraftSoundCapture)(Object)this); }
    @Inject(method="end",at=@At("HEAD"))
    private void bbstacz$end(CallbackInfo ci) { NativeVideoSounds.end((MinecraftSoundCapture)(Object)this); }
    @Inject(method="capture",at=@At("HEAD"))
    private void bbstacz$before(SoundInstance instance,CallbackInfo ci) { bbstacz$before=((MinecraftSoundCapture)(Object)this).getSounds().size(); }
    @Inject(method="capture",at=@At("RETURN"))
    private void bbstacz$remember(SoundInstance instance,CallbackInfo ci) {
        var capture=(MinecraftSoundCapture)(Object)this; var rows=capture.getSounds();
        if(instance instanceof GunSoundInstance && rows.size()>bbstacz$before) NativeVideoSounds.capture(capture,instance,rows.get(rows.size()-1));
    }
}
