package mcaxolotl.bbstacz.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kosmx.playerAnim.core.util.Pair;
import dev.kosmx.playerAnim.impl.Helper;
import dev.kosmx.playerAnim.impl.IUpperPartHelper;
import dev.kosmx.playerAnim.impl.animation.IBendHelper;
import java.util.Map;
import mcaxolotl.bbstacz.recording.PartPose;
import mchorse.bbs_mod.forms.renderers.mob.MobRig;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.mixin.client.LivingEntityRendererInvoker;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The same body/layer and wrist bend frames that PA uses beyond vanilla ModelPart's rigid transforms. */
public final class NativeBends {
    private NativeBends() {}
    public static void body(PoseStack pose, Map<String, PartPose> parts) {
        var bend = parts.get("bend/body");
        if (Helper.isBendEnabled() && bend != null) IBendHelper.rotateMatrixStack(pose, new Pair<>(bend.at(3), bend.at(4)));
    }
    public static void wrist(PoseStack pose, Map<String, PartPose> parts) {
        var bend = parts.get("bend/right_arm");
        if (Helper.isBendEnabled() && bend != null) {
            pose.translate(0, 0.25, 0);
            pose.mulPose(new Quaternionf().rotateAxis(bend.at(4), new Vector3f((float)Math.cos(-bend.at(3)), 0, (float)Math.sin(-bend.at(3)))));
            pose.translate(0, -0.25, 0);
        }
    }
    public static void item(PoseStack pose, Map<String, PartPose> parts) {
        var item = parts.get("item/right"); if (item == null) return;
        pose.translate(item.at(6) / 16f, item.at(7) / 16f, item.at(8) / 16f);
        pose.mulPose(com.mojang.math.Axis.ZP.rotation(item.at(5))); pose.mulPose(com.mojang.math.Axis.YP.rotation(item.at(4))); pose.mulPose(com.mojang.math.Axis.XP.rotation(item.at(3)));
    }
    public static void matrices(PlayerRenderer renderer, AbstractClientPlayer player, MobRig rig, Map<String, PartPose> parts, float delta, MatrixCache cache) {
        var bend = parts.get("bend/body"); if (!Helper.isBendEnabled() || bend == null || bend.at(4) == 0) return;
        PoseStack root = new PoseStack(); var access = (LivingEntityRendererInvoker) renderer;
        access.bbs$setupTransforms(player, root, player.tickCount + delta, 0, delta);
        root.scale(-1, -1, 1); access.bbs$scale(player, root, delta); root.translate(0, -1.501, 0);
        Matrix4f inverse = new Matrix4f(root.last().pose()).invert(); body(root, parts);
        Matrix4f correction = new Matrix4f(root.last().pose()).mul(inverse);
        for (var part : rig.ordered()) {
            if ((Object) part instanceof IUpperPartHelper helper && helper.isUpperPart()) {
                String name = rig.name(part); var entry = cache.get(name);
                if (entry.matrix() != null) cache.put(name, new Matrix4f(correction).mul(entry.matrix()), new Matrix4f(correction).mul(entry.origin()));
            }
        }
    }
}
