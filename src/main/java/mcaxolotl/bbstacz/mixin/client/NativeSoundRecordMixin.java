package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.client.sound.GunSoundInstance;
import com.tacz.guns.client.sound.SoundPlayManager;
import mcaxolotl.bbstacz.client.RecordedAudio;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The final overload has already resolved SoundData's random volume/pitch and the real resource. */
@Mixin(value = SoundPlayManager.class, remap = false)
public abstract class NativeSoundRecordMixin {
    @Inject(method = "playClientSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/resources/ResourceLocation;FFIZIZZ)Lcom/tacz/guns/client/sound/GunSoundInstance;", at = @At("HEAD"), cancellable = true)
    private static void bbstacz$duplicate(Entity entity, ResourceLocation name, float volume, float pitch, int distance, boolean mono, int limit, boolean track, boolean relative, CallbackInfoReturnable<GunSoundInstance> ci) {
        if (RecordedAudio.suppress(entity)) ci.setReturnValue(null);
    }
    @Inject(method = "playClientSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/resources/ResourceLocation;FFIZIZZ)Lcom/tacz/guns/client/sound/GunSoundInstance;", at = @At("RETURN"))
    private static void bbstacz$record(Entity entity, ResourceLocation name, float volume, float pitch, int distance, boolean mono, int limit, boolean track, boolean relative, CallbackInfoReturnable<GunSoundInstance> ci) {
        RecordedAudio.capture(entity, ci.getReturnValue());
    }
}
