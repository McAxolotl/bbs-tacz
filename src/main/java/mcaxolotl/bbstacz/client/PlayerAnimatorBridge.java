package mcaxolotl.bbstacz.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import java.util.Map;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import dev.kosmx.playerAnim.core.util.Pair;
import dev.kosmx.playerAnim.impl.animation.IBendHelper;
import mcaxolotl.bbstacz.recording.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;

/** Loaded only if player-animator is installed. Sampling does not tick the player's animation stack. */
public final class PlayerAnimatorBridge {
    private PlayerAnimatorBridge() {}
    public static void prepare(AbstractClientPlayer player, float delta) {
        ((IAnimatedPlayer) player).playerAnimator_getAnimation().setTickDelta(delta);
    }
    public static void applyBends(PlayerModel<?> model, Map<String, PartPose> poses) {
        bend(poses.get("bend/torso"), model.body, model.jacket);
        bend(poses.get("bend/right_arm"), model.rightArm, model.rightSleeve); bend(poses.get("bend/left_arm"), model.leftArm, model.leftSleeve);
        bend(poses.get("bend/right_leg"), model.rightLeg, model.rightPants); bend(poses.get("bend/left_leg"), model.leftLeg, model.leftPants);
    }
    private static void bend(PartPose pose, ModelPart... parts) {
        Pair<Float, Float> value = new Pair<>(pose == null ? 0f : pose.at(3), pose == null ? 0f : pose.at(4));
        for (ModelPart part : parts) IBendHelper.INSTANCE.bend(part, value);
    }
    public static void captureBody(AbstractClientPlayer player, Map<String, PartPose> poses) {
        var animation = ((IAnimatedPlayer) player).playerAnimator_getAnimation();
        if (!animation.isActive()) return;
        Vec3f p = animation.get3DTransform("body", TransformType.POSITION, Vec3f.ZERO);
        Vec3f r = animation.get3DTransform("body", TransformType.ROTATION, Vec3f.ZERO);
        for (String[] role : new String[][]{{"torso","torso"},{"right_arm","rightArm"},{"left_arm","leftArm"},{"right_leg","rightLeg"},{"left_leg","leftLeg"}}) {
            var bend = animation.getBend(role[1]);
            float axis = bend == null ? 0 : bend.getLeft(), angle = bend == null ? 0 : bend.getRight();
            if (role[0].equals("torso")) { var body = animation.getBend("body"); if (body != null) { axis += body.getLeft(); angle += body.getRight(); } }
            poses.put("bend/" + role[0], new PartPose(new float[]{0,0,0,axis,angle,0,0,0,0,1,1,1,0,0,0,1}, true, false));
        }
        var bodyBend = animation.getBend("body");
        poses.put("bend/body", new PartPose(new float[]{0,0,0,bodyBend.getLeft(),bodyBend.getRight(),0,0,0,0,1,1,1,0,0,0,1}, true, false));
        for (String hand : new String[]{"right", "left"}) {
            Vec3f ip = animation.get3DTransform(hand + "Item", TransformType.POSITION, Vec3f.ZERO);
            Vec3f ir = animation.get3DTransform(hand + "Item", TransformType.ROTATION, Vec3f.ZERO);
            poses.put("item/" + hand, new PartPose(new float[]{0,0,0,ir.getX(),ir.getY(),ir.getZ(),ip.getX(),ip.getY(),ip.getZ(),1,1,1,0,0,0,1}, true, false));
        }
        poses.put("anchor", new PartPose(new float[]{0, 0, 0, r.getX(), r.getY(), r.getZ(), p.getX(), p.getY(), p.getZ(), 1, 1, 1, 0, 0, 0, 1}, true, false));
    }
}
