package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.client.sound.GunSoundInstance;
import com.tacz.guns.client.sound.SoundPlayManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value=SoundPlayManager.class, remap=false)
public interface NativeSoundPlayAccess {
    @Invoker("playClientSound")
    static GunSoundInstance bbstacz$play(Entity entity,ResourceLocation name,float volume,float pitch,int distance,boolean mono,int limit,boolean tracking,boolean relative) { throw new AssertionError(); }
}
