package mcaxolotl.bbstacz.smoke;

import java.util.function.Consumer;
import mcaxolotl.bbstacz.client.NativeRenderScope;
import net.minecraft.client.model.PlayerModel;

/** Observations taken at the actual model draw, not from values left after model-state restoration. */
public final class ReviewProbe {
    public record Pose(NativeRenderScope scope, float rightX, float rightY, float rightZ, float leftX, float leftY, float leftZ, int texture) {}
    public static Consumer<Pose> playerDraw;
    public static Consumer<String> recordedGunDraw;
    public static Consumer<com.tacz.guns.api.entity.ShootResult> shootResult;
    public static long liveGunDraws;
    public static String exportFilm;
    public static int firstExportTick=-1;
    public static boolean failFirstFrame;
    public static int exportSleepMillis;
    public static volatile int serverSuccessfulShots;
    public static double firstWorldTime=Double.NaN, lastWorldTime;
    public static long firstGunTime, lastGunTime;
    public static void resetClock() { firstWorldTime=Double.NaN; }
    public static void clock() {
        var recorder=mchorse.bbs_mod.BBSModClient.getVideoRecorder();
        double world=recorder.serverTicks+net.minecraft.client.Minecraft.getInstance().getFrameTime();
        long gun=com.tacz.guns.util.time.GunTime.nowNanos();
        if (Double.isNaN(firstWorldTime)) { firstWorldTime=world; firstGunTime=gun; }
        lastWorldTime=world; lastGunTime=gun;
    }
    public static void requireFramed(net.minecraft.client.Minecraft client,mchorse.bbs_mod.forms.entities.IEntity actor) {
        var camera=client.gameRenderer.getMainCamera(); var position=camera.getPosition(); var direction=camera.getLookVector();
        var delta=new net.minecraft.world.phys.Vec3(actor.getX(),actor.getY()+0.9,actor.getZ()).subtract(position);
        double alignment=delta.normalize().dot(new net.minecraft.world.phys.Vec3(direction.x,direction.y,direction.z));
        ReviewClient.require(delta.length()<10&&alignment>0.85,"TP actor rendered outside the output camera: distance="+delta.length()+" alignment="+alignment+" camera="+position);
    }
    public static float armError(Pose actual,java.util.Map<String,mcaxolotl.bbstacz.recording.PartPose> expected) {
        var right=expected.get("right_arm"); var left=expected.get("left_arm");
        if(right==null||left==null) return Float.POSITIVE_INFINITY;
        float[] angles={actual.rightX(),actual.rightY(),actual.rightZ(),actual.leftX(),actual.leftY(),actual.leftZ()};
        float error=0;
        for(int i=0;i<angles.length;i++) {
            float reference=(i<3?right:left).at(3+i%3),difference=angles[i]-reference;
            difference=(float)Math.atan2(Math.sin(difference),Math.cos(difference)); error=Math.max(error,Math.abs(difference));
        }
        return error;
    }
    public static void player(PlayerModel<?> model) {
        var scope = NativeRenderScope.current();
        if (playerDraw != null && scope != null && !scope.matrices)
            playerDraw.accept(new Pose(scope, model.rightArm.xRot, model.rightArm.yRot, model.rightArm.zRot,
                model.leftArm.xRot, model.leftArm.yRot, model.leftArm.zRot, com.mojang.blaze3d.systems.RenderSystem.getShaderTexture(0)));
    }
}
