package mcaxolotl.bbstacz.client;

import java.util.Map;
import mcaxolotl.bbstacz.recording.PartPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

public final class NativePlayerPose {
    private NativePlayerPose() {}
    public static void apply(PlayerModel<?> model, Map<String, PartPose> poses) {
        apply(model.head, poses.get("head")); apply(model.body, poses.get("torso"));
        apply(model.rightArm, poses.get("right_arm")); apply(model.leftArm, poses.get("left_arm"));
        apply(model.rightLeg, poses.get("right_leg")); apply(model.leftLeg, poses.get("left_leg"));
        model.hat.copyFrom(model.head); model.jacket.copyFrom(model.body); model.rightSleeve.copyFrom(model.rightArm); model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg); model.leftPants.copyFrom(model.leftLeg);
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("player-animator")) PlayerAnimatorBridge.applyBends(model, poses);
    }
    private static void apply(ModelPart part, PartPose pose) {
        if (pose == null) return;
        part.x = pose.at(0); part.y = pose.at(1); part.z = pose.at(2); part.xRot = pose.at(3); part.yRot = pose.at(4); part.zRot = pose.at(5);
        part.xScale = pose.at(9); part.yScale = pose.at(10); part.zScale = pose.at(11); part.visible = pose.visible;
    }
}
