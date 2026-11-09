package mcaxolotl.bbstacz.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.client.event.BeforeRenderHandEvent;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import cn.sh1rocu.simplebedrockmodel.api.event.ViewportEvent;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import mcaxolotl.bbstacz.client.LiveCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunItemRendererWrapper.class, remap = false)
public abstract class GunRendererMixin {
    @Unique private Matrix4f bbstacz$cameraBefore;
    @Inject(method = "renderFirstPerson", at = @At("HEAD"), cancellable = true)
    private void bbstacz$render(LocalPlayer player, ItemStack gun, ItemDisplayContext mode, PoseStack pose, MultiBufferSource buffers,
                               int light, float delta, CallbackInfo ci) {
        if (FirstPersonPlayback.render(pose, buffers, light, mode, delta)) ci.cancel(); else LiveCapture.begin(pose);
    }
    @Inject(method = "renderFirstPerson", at = @At("RETURN"))
    private void bbstacz$end(LocalPlayer player, ItemStack gun, ItemDisplayContext mode, PoseStack pose, MultiBufferSource buffers,
                            int light, float delta, CallbackInfo ci) { LiveCapture.end(); }
    @Inject(method = "applyItemInHandCameraAnimation", at = @At("HEAD"), cancellable = true)
    private void bbstacz$hand(BeforeRenderHandEvent event, ItemStack stack, LocalPlayer player, CallbackInfo ci) {
        var target = FirstPersonPlayback.target(Minecraft.getInstance().getFrameTime());
        if (target != null) {
            float[] q = target.frame().handCamera(); event.getPoseStack().mulPose(new Quaternionf(q[0], q[1], q[2], q[3])); ci.cancel();
        } else bbstacz$cameraBefore = new Matrix4f(event.getPoseStack().last().pose()).invert();
    }
    @Inject(method = "applyItemInHandCameraAnimation", at = @At("RETURN"))
    private void bbstacz$saveHand(BeforeRenderHandEvent event, ItemStack stack, LocalPlayer player, CallbackInfo ci) {
        if (bbstacz$cameraBefore != null) {
            LiveCapture.camera(new Matrix4f(bbstacz$cameraBefore).mul(event.getPoseStack().last().pose()).getNormalizedRotation(new Quaternionf())); bbstacz$cameraBefore = null;
        }
    }
    @Inject(method = "applyLevelCameraAnimation", at = @At("HEAD"), cancellable = true)
    private void bbstacz$camera(ViewportEvent.ComputeCameraAngles event, ItemStack stack, LocalPlayer player, CallbackInfo ci) {
        if (FirstPersonPlayback.target(Minecraft.getInstance().getFrameTime()) != null) ci.cancel(); // BBS's recorded camera is authoritative.
    }
}
