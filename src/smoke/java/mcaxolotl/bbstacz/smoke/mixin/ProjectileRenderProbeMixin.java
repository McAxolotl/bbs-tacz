package mcaxolotl.bbstacz.smoke.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.entity.EntityBulletRenderer;
import com.tacz.guns.entity.EntityKineticBullet;
import mcaxolotl.bbstacz.smoke.ProjectileProbe;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=EntityBulletRenderer.class,remap=false)
public abstract class ProjectileRenderProbeMixin {
    @Inject(method="render(Lcom/tacz/guns/entity/EntityKineticBullet;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("HEAD"))
    private void bbstacz$draw(EntityKineticBullet bullet,float yaw,float delta,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci) { ProjectileProbe.draw(bullet); }
}
