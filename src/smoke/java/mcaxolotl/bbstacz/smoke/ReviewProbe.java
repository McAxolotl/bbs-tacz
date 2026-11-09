package mcaxolotl.bbstacz.smoke;

import java.util.function.Consumer;
import mcaxolotl.bbstacz.client.NativeRenderScope;
import net.minecraft.client.model.PlayerModel;

/** Observations taken at the actual model draw, not from values left after model-state restoration. */
public final class ReviewProbe {
    public record Pose(NativeRenderScope scope, float rightX, float rightY, float rightZ, float leftX, float leftY, float leftZ, int texture) {}
    public static Consumer<Pose> playerDraw;
    public static long liveGunDraws;
    public static String exportFilm;
    public static int firstExportTick=-1;
    public static boolean failFirstFrame;
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
    public static void player(PlayerModel<?> model) {
        var scope = NativeRenderScope.current();
        if (playerDraw != null && scope != null && !scope.matrices)
            playerDraw.accept(new Pose(scope, model.rightArm.xRot, model.rightArm.yRot, model.rightArm.zRot,
                model.leftArm.xRot, model.leftArm.yRot, model.leftArm.zRot, com.mojang.blaze3d.systems.RenderSystem.getShaderTexture(0)));
    }
}
