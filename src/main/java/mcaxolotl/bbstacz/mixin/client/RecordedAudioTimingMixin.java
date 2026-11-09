package mcaxolotl.bbstacz.mixin.client;

import mcaxolotl.bbstacz.client.RecordedAudio;
import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.film.WorldFilmController;
import mchorse.bbs_mod.utils.clips.ClipContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** BBS's world audio context otherwise stays playing=true while its film clock is paused. */
@Mixin(value = WorldFilmController.class, remap = false)
public abstract class RecordedAudioTimingMixin {
    @Unique private boolean bbstacz$managing, bbstacz$savedPlaying;
    @Inject(method = "render", at = @At("HEAD"))
    private void bbstacz$pause(WorldRenderContext render, CallbackInfo ci) {
        var controller = (WorldFilmController)(Object)this;
        RecordedAudio.playback(controller,render.tickDelta());
        var context = controller.getContext();
        if (RecordedAudio.hasAnyTrack(controller.film)) {
            if (!bbstacz$managing) bbstacz$savedPlaying = context.playing;
            bbstacz$managing = true; context.playing = !controller.paused && !Minecraft.getInstance().isPaused();
        } else if (bbstacz$managing) {
            context.playing = bbstacz$savedPlaying; bbstacz$managing = false;
        }
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/camera/clips/CameraClipContext;setup(IF)Lmchorse/bbs_mod/utils/clips/ClipContext;"))
    private ClipContext bbstacz$time(CameraClipContext context, int tick, float delta) {
        return context.setup(tick, bbstacz$managing && !context.playing ? 0 : delta);
    }
}
