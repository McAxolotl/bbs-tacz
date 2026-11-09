package mcaxolotl.bbstacz.smoke;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.nio.file.Path;
import mcaxolotl.bbstacz.client.forms.TaczFormRenderer;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/** Pixel smoke test and inspectable evidence. Renders only into a disposable framebuffer, never a user's screenshot folder. */
final class NativePreviewSmoke {
    static int render(Minecraft client, TaczFormRenderer renderer, IEntity source, String file, boolean crawl) throws Exception {
        return render(client, renderer, source, file, crawl, 35);
    }
    static int render(Minecraft client, TaczFormRenderer renderer, IEntity source, String file, boolean crawl, float yaw) throws Exception {
        var target = new TextureTarget(512, 512, true, Minecraft.ON_OSX);
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix()); var sorting = RenderSystem.getVertexSorting();
        var lightField = RenderSystem.class.getDeclaredField("shaderLightDirections"); lightField.setAccessible(true); var lights = (org.joml.Vector3f[]) lightField.get(null); var l0 = new org.joml.Vector3f(lights[0]); var l1 = new org.joml.Vector3f(lights[1]);
        var view = RenderSystem.getModelViewStack(); view.pushPose(); view.setIdentity(); RenderSystem.applyModelViewMatrix();
        try {
            target.setClearColor(0.055f, 0.065f, 0.085f, 1); target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-1.45f, 1.45f, -0.15f, 2.75f, 0.05f, 100), VertexSorting.ORTHOGRAPHIC_Z);
            Lighting.setupForEntityInInventory(); client.gameRenderer.lightTexture().turnOnLightLayer();
            PoseStack pose = new PoseStack(); pose.translate(0, crawl ? 0.6 : 0.15, -8); pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
            renderer.render(new FormRenderingContext().set(null, source, pose, 15728880, OverlayTexture.NO_OVERLAY, 0.5f));
            try (var image = Screenshot.takeScreenshot(target)) {
                int pixels = 0; int background = image.getPixelRGBA(0, 0);
                for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) if (image.getPixelRGBA(x,y) != background) pixels++;
                image.writeToFile(Path.of(file)); return pixels;
            }
        } finally {
            client.gameRenderer.lightTexture().turnOffLightLayer(); target.destroyBuffers(); client.getMainRenderTarget().bindWrite(true);
            view.popPose(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(projection, sorting); RenderSystem.setShaderLights(l0, l1);
        }
    }
}
