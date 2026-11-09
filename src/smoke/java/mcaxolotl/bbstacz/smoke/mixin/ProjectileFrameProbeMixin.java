package mcaxolotl.bbstacz.smoke.mixin;

import mcaxolotl.bbstacz.smoke.ProjectileProbe;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class ProjectileFrameProbeMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void bbstacz$frame(float delta,long time,boolean level,CallbackInfo ci) {
        ProjectileProbe.frame();

    }
}
