package mcaxolotl.bbstacz.mixin.client;

import java.util.List;
import mcaxolotl.bbstacz.client.NativeCapturedSound;
import mchorse.bbs_mod.audio.MinecraftSoundCapture;
import mchorse.bbs_mod.audio.Wave;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** BBS normally honors endFrame for loops only; a native reload/inspect one-shot can also be interrupted. */
@Mixin(targets="mchorse.bbs_mod.audio.MinecraftSoundMixer$SoundCursor",remap=false)
public abstract class NativeVideoSoundEndMixin {
    @Shadow @Final @Mutable private long end;
    @Inject(method="<init>",at=@At("TAIL"))
    private void bbstacz$interrupted(MinecraftSoundCapture.CapturedSound sound,Wave wave,List<MinecraftSoundCapture.ListenerFrame> frames,int sampleRate,double fps,long total,CallbackInfo ci) {
        if(((NativeCapturedSound)sound).bbstacz$isNativeGun() && !sound.loop && sound.endFrame>=0)
            end=Math.min(end,Math.round(sound.endFrame/fps*sampleRate));
    }
}
