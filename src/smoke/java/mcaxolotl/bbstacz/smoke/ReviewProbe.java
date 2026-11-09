package mcaxolotl.bbstacz.smoke;

import java.util.function.Consumer;
import mcaxolotl.bbstacz.client.NativeRenderScope;
import net.minecraft.client.model.PlayerModel;

/** Observations taken at the actual model draw, not from values left after model-state restoration. */
public final class ReviewProbe {
    public record Pose(NativeRenderScope scope, float rightX, float rightY, float rightZ, float leftX, float leftY, float leftZ, int texture) {}
    public static Consumer<Pose> playerDraw;
    public static long liveGunDraws;
    public static void player(PlayerModel<?> model) {
        var scope = NativeRenderScope.current();
        if (playerDraw != null && scope != null && !scope.matrices)
            playerDraw.accept(new Pose(scope, model.rightArm.xRot, model.rightArm.yRot, model.rightArm.zRot,
                model.leftArm.xRot, model.leftArm.yRot, model.leftArm.zRot, com.mojang.blaze3d.systems.RenderSystem.getShaderTexture(0)));
    }
}
