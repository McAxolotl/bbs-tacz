package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.client.sound.GunSoundInstance;
import mcaxolotl.bbstacz.client.RecordedAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Native gun attenuation must use the actual film/editor listener, not the spectator left outside the shot. */
@Mixin(value=GunSoundInstance.class,remap=false)
public abstract class ReplaySoundListenerMixin {
    @Redirect(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;distanceToSqr(DDD)D",remap=true))
    private double bbstacz$filmListener(LocalPlayer player,double x,double y,double z) {
        if(RecordedAudio.spatialEmitting()) return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(x,y,z);
        return player.distanceToSqr(x,y,z);
    }
}
