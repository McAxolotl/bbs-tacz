package mcaxolotl.bbstacz.client;

import java.util.IdentityHashMap;
import java.util.Map;
import mchorse.bbs_mod.audio.MinecraftSoundCapture;
import net.minecraft.client.resources.sounds.SoundInstance;

/** Associates native one-shot stops with BBS's temporary sound-capture rows for video post-mixing. */
public final class NativeVideoSounds {
    private static MinecraftSoundCapture owner;
    private static final Map<SoundInstance,MinecraftSoundCapture.CapturedSound> ACTIVE=new IdentityHashMap<>();
    private NativeVideoSounds() {}
    public static void begin(MinecraftSoundCapture capture) { owner=capture; ACTIVE.clear(); }
    public static void capture(MinecraftSoundCapture capture,SoundInstance instance,MinecraftSoundCapture.CapturedSound row) {
        if(owner!=capture) return;
        ((NativeCapturedSound)row).bbstacz$nativeGun(true); ACTIVE.put(instance,row);
    }
    public static void stopped(SoundInstance sound) {
        var row=ACTIVE.remove(sound);
        if(row==null || owner==null || !owner.isActive()) return;
        int end=owner.getFrames().size(); row.endFrame=row.endFrame<0?end:Math.min(row.endFrame,end);
    }
    public static void end(MinecraftSoundCapture capture) { if(owner==capture) { ACTIVE.clear(); owner=null; } }
    public static void clear() { ACTIVE.clear(); owner=null; }
}
