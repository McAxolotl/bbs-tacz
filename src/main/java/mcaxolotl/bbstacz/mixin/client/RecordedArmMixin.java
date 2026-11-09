package mcaxolotl.bbstacz.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import mcaxolotl.bbstacz.client.forms.TaczVisualPlayer;
import mcaxolotl.bbstacz.forms.TaczForm;
import mchorse.bbs_mod.forms.FormUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Recorded gun hands use the replay form's skin/arm width, not the viewer's current morph. */
@Mixin(value = PlayerRenderer.class, priority = 1100)
public abstract class RecordedArmMixin {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void bbstacz$form(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        if (!FirstPersonPlayback.isRendering() || player instanceof TaczVisualPlayer) return;
        var target = FirstPersonPlayback.target(Minecraft.getInstance().getFrameTime()); if (target == null) return;
        var actor = target.controller().getEntities().get(target.replay().getId());
        var form = actor != null && actor.getForm() instanceof TaczForm ? actor.getForm() : target.replay().form.get();
        if (!(form instanceof TaczForm)) return;
        var renderer = FormUtilsClient.getRenderer(form);
        var hand = ((PlayerRenderer)(Object)this).getModel().rightArm == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (renderer != null && renderer.renderArm(pose, light, player, hand)) ci.cancel();
    }
}
