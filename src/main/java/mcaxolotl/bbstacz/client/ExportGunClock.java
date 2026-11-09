package mcaxolotl.bbstacz.client;

import mcaxolotl.bbstacz.recording.ExportTimebase;
import mchorse.bbs_mod.BBSModClient;
import net.minecraft.client.Minecraft;

/** One clock for native gun use, SBM runners, ADS, muzzle flash and clock-driven gun springs. */
public final class ExportGunClock {
    private static final ThreadLocal<Long> OVERRIDE=new ThreadLocal<>();
    private static final ExportTimebase TIME=new ExportTimebase();
    private ExportGunClock() {}
    public static long sample(long raw) {
        Long time=OVERRIDE.get(); if(time!=null) return time;
        var video=BBSModClient.getVideoRecorder();
        boolean active=video!=null && video.isRecording();
        // BBS advances this coordinate only for actual output frames (not held/throttled frames).
        long nanos=active ? Math.round((video.serverTicks+(double)Minecraft.getInstance().getFrameTime())*50_000_000d) : 0;
        return TIME.sample(raw,active,nanos);
    }
    public static void atElapsedOffset(long nanos,Runnable action) {
        long now=com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.PausedClientAnimationClock.getInstance().nowNanos();
        Long previous=OVERRIDE.get(); OVERRIDE.set(Math.max(0,now-nanos));
        try { action.run(); } finally { if(previous==null) OVERRIDE.remove(); else OVERRIDE.set(previous); }
    }
    public static void reset() { TIME.reset(); }
}
