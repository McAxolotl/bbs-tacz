package mcaxolotl.bbstacz.client;

import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import java.util.LinkedHashMap;
import java.util.Map;
import mcaxolotl.bbstacz.recording.PartPose;
import net.minecraft.client.model.geom.ModelPart;

public final class NativePose {
    private NativePose() {}
    public static PartPose capture(BedrockPart p) {
        return new PartPose(new float[]{p.x, p.y, p.z, p.xRot, p.yRot, p.zRot, p.offsetX, p.offsetY, p.offsetZ,
            p.xScale, p.yScale, p.zScale, p.additionalQuaternion.x, p.additionalQuaternion.y, p.additionalQuaternion.z, p.additionalQuaternion.w}, p.visible, p.illuminated);
    }
    public static PartPose capture(ModelPart p) {
        return new PartPose(new float[]{p.x, p.y, p.z, p.xRot, p.yRot, p.zRot, 0, 0, 0, p.xScale, p.yScale, p.zScale, 0, 0, 0, 1}, p.visible, false);
    }
    public static Map<String, PartPose> capture(BedrockGunModel model) {
        Map<String, PartPose> out = new LinkedHashMap<>();
        // Includes disconnected functional roots, not just the render tree's root.
        for (String name : model.getIndexBones().keySet()) {
            BedrockPart part = model.getNode(name);
            if (part != null) out.put(name, capture(part));
        }
        return out;
    }
    public static void apply(BedrockGunModel model, Map<String, PartPose> snapshot) {
        snapshot.forEach((name, p) -> {
            BedrockPart part = model.getNode(name);
            if (part == null) return; // Changed gun pack: never invent bones or crash the render.
            part.x = p.at(0); part.y = p.at(1); part.z = p.at(2);
            part.xRot = p.at(3); part.yRot = p.at(4); part.zRot = p.at(5);
            part.offsetX = p.at(6); part.offsetY = p.at(7); part.offsetZ = p.at(8);
            part.xScale = p.at(9); part.yScale = p.at(10); part.zScale = p.at(11);
            part.additionalQuaternion.set(p.at(12), p.at(13), p.at(14), p.at(15));
            part.visible = p.visible; part.illuminated = p.illuminated;
        });
    }
}
