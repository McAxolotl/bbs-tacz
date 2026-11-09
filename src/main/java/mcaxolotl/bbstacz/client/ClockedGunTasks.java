package mcaxolotl.bbstacz.client;

import com.tacz.guns.util.time.GunTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import mcaxolotl.bbstacz.recording.ClockedTaskQueue;
import net.minecraft.client.Minecraft;

/** Native local semi/burst scheduling follows the same paused/video clock as its gun animations. */
public final class ClockedGunTasks {
    private static final ClockedTaskQueue TASKS=new ClockedTaskQueue(GunTime::nowNanos);
    private static boolean running;
    private ClockedGunTasks() {}
    public static ScheduledFuture<?> schedule(Runnable action,long delay,long period,TimeUnit unit) { return TASKS.schedule(action,delay,period,unit); }
    public static boolean running() { return running; }
    public static void pump() {
        var client=Minecraft.getInstance(); if(client==null || !client.isSameThread() || running || !GunTime.shouldTick()) return;
        long now=GunTime.nowNanos(); running=true;
        try {
            boolean video=mchorse.bbs_mod.BBSModClient.getVideoRecorder().isRecording();
            TASKS.pump(now,(due,action)-> { if(video) ExportGunClock.atElapsedOffset(Math.max(0,now-due),action); else action.run(); });
        }
        finally { running=false; }
    }
    public static void clear() { TASKS.clear(); }
}
