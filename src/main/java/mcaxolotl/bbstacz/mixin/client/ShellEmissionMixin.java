package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.client.resource.GunDisplayInstance;
import mcaxolotl.bbstacz.client.CommandJournal;
import mcaxolotl.bbstacz.client.ShellIds;
import mcaxolotl.bbstacz.client.ThirdPersonShells;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunAnimationStateContext.class, remap = false)
public abstract class ShellEmissionMixin {
    @Shadow private GunDisplayInstance display;
    @Inject(method = "popShellFrom", at = @At("TAIL"))
    private void bbstacz$shell(int origin, CallbackInfo ci) {
        if (display == null || display.getGunModel() == null) return;
        ShellRender renderer = display.getGunModel().getShellRender(origin); if (renderer == null) return;
        ShellRender.Data data = ((ShellQueueAccess) renderer).bbstacz$shells().peekLast(); if (data == null) return;
        mcaxolotl.bbstacz.client.CapturedShells.emitted(origin, Minecraft.getInstance().player.getMainHandItem(), data);
        long id = ShellIds.of(data);
        ThirdPersonShells.queue(Minecraft.getInstance().player, origin, id, data.timeStamp, data.randomOffset);
        CommandJournal.capture("pop_shell", origin, id, data.randomOffset.x, data.randomOffset.y, data.randomOffset.z);
    }
}
