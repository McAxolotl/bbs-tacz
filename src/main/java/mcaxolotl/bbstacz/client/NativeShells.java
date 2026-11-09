package mcaxolotl.bbstacz.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.GunDisplayInstance;
import mcaxolotl.bbstacz.recording.ShellSnapshot;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Native shell mesh and TACZ's analytical ballistic motion, without its shared per-display queue. */
public final class NativeShells {
    private NativeShells() {}
    public static void render(GunDisplayInstance display, ItemStack gunStack, ShellSnapshot shell, Matrix4f base,
                              MultiBufferSource buffers, int light, int overlay, ItemDisplayContext mode) {
        var ejection = display.getShellEjection();
        if (ejection == null || shell.ageMillis() < 0 || shell.ageMillis() > ejection.getLivingTime() * 1000) return;
        var model = display.getShellModel(); var texture = display.getShellTextureLocation();
        if (model == null || texture == null) {
            IGun gun = IGun.getIGunOrNull(gunStack); if (gun == null) return;
            var index = TimelessAPI.getClientGunIndex(gun.getGunId(gunStack)).orElse(null); if (index == null) return;
            var ammo = TimelessAPI.getClientAmmoIndex(index.getGunData().getAmmoId()).orElse(null); if (ammo == null) return;
            model = ammo.getShellModel(); texture = ammo.getShellTextureLocation();
        }
        if (model == null || texture == null) return;
        PoseStack pose = new PoseStack();
        Matrix4f matrix = new Matrix4f(base).mul(new Matrix4f().set(shell.emissionMatrix()));
        pose.last().pose().set(matrix); pose.last().normal().set(new Matrix3f(matrix).invert().transpose());
        double time = shell.ageMillis() / 1000d;
        var v = ejection.getInitialVelocity(); var a = ejection.getAcceleration(); var w = ejection.getAngularVelocity(); var r = shell.randomVelocity();
        pose.translate(-(v.x + r[0]) * time - a.x * time * time / 2,
            -(v.y + r[1]) * time - a.y * time * time / 2, (v.z + r[2]) * time + a.z * time * time / 2);
        pose.mulPose(Axis.XN.rotationDegrees((float) time * w.x)); pose.mulPose(Axis.YN.rotationDegrees((float) time * w.y));
        pose.mulPose(Axis.ZP.rotationDegrees((float) time * w.z)); pose.translate(0, -1.5, 0);
        model.render(pose, mode, RenderType.entityCutout(texture), light, overlay, buffers);
    }
}
