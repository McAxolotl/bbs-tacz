package mcaxolotl.bbstacz.client;

import mcaxolotl.bbstacz.recording.ExportTimebase;
import mchorse.bbs_mod.BBSModClient;
import net.minecraft.client.Minecraft;

/** One clock for native gun use, SBM runners, ADS, muzzle flash and clock-driven gun springs. */
public final class ExportGunClock {
    private static final ExportTimebase TIME=new ExportTimebase();
    private ExportGunClock() {}
    public static long sample(long raw) {
        var video=BBSModClient.getVideoRecorder();
        boolean active=video!=null && video.isRecording();
        // BBS advances this coordinate only for actual output frames (not held/throttled frames).
        long nanos=active ? Math.round((video.serverTicks+(double)Minecraft.getInstance().getFrameTime())*50_000_000d) : 0;
        return TIME.sample(raw,active,nanos);
    }
    public static void reset() { TIME.reset(); }
}
