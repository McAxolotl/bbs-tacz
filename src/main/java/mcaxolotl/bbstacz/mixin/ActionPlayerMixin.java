package mcaxolotl.bbstacz.mixin;

import java.util.Map;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.PhysicalCrawl;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.actions.ActionPlayer;
import mchorse.bbs_mod.film.replays.Replay;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ActionPlayer.class, remap = false)
public abstract class ActionPlayerMixin {
    @Shadow private Map<String, LivingEntity> actors;
    @Shadow public mchorse.bbs_mod.film.Film film;
    @Inject(method = "apply", at = @At("TAIL"))
    private void bbstacz$crawl(LivingEntity actor, Replay replay, float tick, boolean ticking, CallbackInfo ci) {
        var channel = ((TaczReplayChannels) replay.keyframes).bbstacz$state();
        if (!channel.isEmpty()) PhysicalCrawl.apply(actor, this, channel.interpolate(replay.getTick((int) tick), FrameSnapshot.EMPTY).crawling());
    }
    @Inject(method = "stop", at = @At("HEAD"))
    private void bbstacz$restore(CallbackInfo ci) { mcaxolotl.bbstacz.recording.ReplayProjectiles.clear(film); actors.values().forEach(actor -> PhysicalCrawl.release(actor, this)); }
    @Inject(method = "goTo(II)V", at = @At("HEAD"))
    private void bbstacz$clearOldFlight(int from,int to,CallbackInfo ci) {
        if (from!=to) mcaxolotl.bbstacz.recording.ReplayProjectiles.clear(film);
    }
    @Redirect(method = "goTo(II)V", at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/actions/ActionPlayer;applyAction()V"))
    private void bbstacz$seekWithoutTransientShots(ActionPlayer player) {
        mcaxolotl.bbstacz.recording.ReplayActionScope.seek(() -> ((ActionPlayerSeekAccess)player).bbstacz$applySeekActions());
    }

}
