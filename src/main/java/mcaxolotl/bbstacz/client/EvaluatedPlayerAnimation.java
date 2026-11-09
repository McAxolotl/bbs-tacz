package mcaxolotl.bbstacz.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import java.util.Map;
import mcaxolotl.bbstacz.client.forms.TaczVisualPlayer;
import mcaxolotl.bbstacz.recording.PartPose;
import net.minecraft.resources.ResourceLocation;

/** A frame-scoped native PA layer for body bend, arm bend and item transforms. It has no tick or playback clock. */
final class EvaluatedPlayerAnimation implements IAnimation {
    private static final ResourceLocation ID = new ResourceLocation("bbs_tacz", "evaluated_player");
    private Map<String, PartPose> poses = Map.of();
    static Runnable bind(TaczVisualPlayer player, Map<String, PartPose> poses) {
        var data = PlayerAnimationAccess.getPlayerAssociatedData(player);
        var found = data.get(ID);
        EvaluatedPlayerAnimation layer;
        if (found instanceof EvaluatedPlayerAnimation existing) layer = existing;
        else { layer = new EvaluatedPlayerAnimation(); data.set(ID, layer); PlayerAnimationAccess.getPlayerAnimLayer(player).addAnimLayer(10000, layer); }
        var previous = layer.poses; layer.poses = poses;
        boolean previousPass = FirstPersonMode.isFirstPersonPass(); FirstPersonMode.setFirstPersonPass(false);
        EvaluatedPlayerAnimation restore = layer;
        return () -> { restore.poses = previous; FirstPersonMode.setFirstPersonPass(previousPass); };
    }
    @Override public void tick() {}
    @Override public void setupAnim(float delta) {}
    @Override public boolean isActive() { return !poses.isEmpty() && !ThirdPersonPose.isSampling(); }
    @Override public FirstPersonMode getFirstPersonMode(float delta) { return FirstPersonMode.DISABLED; }
    @Override public Vec3f get3DTransform(String name, TransformType type, float delta, Vec3f fallback) {
        if (poses.isEmpty() || ThirdPersonPose.isSampling()) return fallback;
        if (type == TransformType.BEND) {
            String role = switch (name) { case "body" -> "body"; case "torso" -> "torso"; case "rightArm" -> "right_arm"; case "leftArm" -> "left_arm"; case "rightLeg" -> "right_leg"; case "leftLeg" -> "left_leg"; default -> null; };
            var bend = role == null ? null : poses.get("bend/" + role);
            float axis = bend == null ? 0 : bend.at(3), angle = bend == null ? 0 : bend.at(4);
            // Native updatePart adds body and torso bends. The recorded torso already contains their sum.
            if ("torso".equals(role)) { var body = poses.get("bend/body"); if (body != null) { axis -= body.at(3); angle -= body.at(4); } }
            return new Vec3f(axis, angle, 0);
        }
        var item = poses.get(name.equals("rightItem") ? "item/right" : name.equals("leftItem") ? "item/left" : "");
        if (item != null) return type == TransformType.ROTATION ? new Vec3f(item.at(3), item.at(4), item.at(5)) : new Vec3f(item.at(6), item.at(7), item.at(8));
        String part = switch (name) { case "head" -> "head"; case "torso" -> "torso"; case "rightArm" -> "right_arm"; case "leftArm" -> "left_arm"; case "rightLeg" -> "right_leg"; case "leftLeg" -> "left_leg"; default -> null; };
        var value = part == null ? null : poses.get(part);
        if (value != null) return type == TransformType.ROTATION ? new Vec3f(value.at(3),value.at(4),value.at(5)) : new Vec3f(value.at(0),value.at(1),value.at(2));
        // NativeBodyRotationMixin owns the full actor anchor; block lower-layer body transforms only.
        return name.equals("body") ? Vec3f.ZERO : fallback;
    }
}
