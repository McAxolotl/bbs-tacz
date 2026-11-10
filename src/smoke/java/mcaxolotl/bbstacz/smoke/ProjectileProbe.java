package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.entity.EntityKineticBullet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.phys.Vec3;

/** Counts real native entities and actual draw calls, distinguishing duplicates from intentional shotgun pellets. */
public final class ProjectileProbe {
    public record Shot(String phase,Vec3 velocity,float yaw,float pitch,float oldYaw,float oldPitch) {}
    public static volatile String phase="ignore";
    public static final CopyOnWriteArrayList<Shot> shots=new CopyOnWriteArrayList<>();
    public static int frame, duplicateDraws, rendered;
    private static final Map<Integer,Integer> drawn=new HashMap<>();
    private static final Map<String,Integer> phaseDraws=new HashMap<>();
    public static void frame() { frame++; drawn.clear(); }
    public static void spawn(EntityKineticBullet bullet) {
        System.out.println("[REVIEW projectile spawn] phase="+phase+" id="+bullet.getId()+" yaw="+bullet.getYRot());
        shots.add(new Shot(phase,bullet.getDeltaMovement(),bullet.getYRot(),bullet.getXRot(),bullet.yRotO,bullet.xRotO));
    }
    public static void draw(EntityKineticBullet bullet) {
        if (!phase.equals("record")&&!phase.equals("ctrl")&&!phase.equals("f6")&&!phase.equals("f4")) return;
        if (phase.equals("f4")&&!mchorse.bbs_mod.BBSModClient.getVideoRecorder().isRecording()) return;
        if(drawn.put(bullet.getId(),frame)!=null) {
            duplicateDraws++;
            if(duplicateDraws<=2) { System.out.println("[REVIEW duplicate tracer] phase="+phase+" frame="+frame+" id="+bullet.getId()+" camera="+net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()); Thread.dumpStack(); }
        }
        rendered++; phaseDraws.merge(phase,1,Integer::sum);
    }
    public static int draws(String phase) { return phaseDraws.getOrDefault(phase,0); }
    public static long count(String phase) { return shots.stream().filter(s->s.phase().equals(phase)).count(); }
}
