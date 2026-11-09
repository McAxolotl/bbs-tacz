package mcaxolotl.bbstacz.mixin;

import mcaxolotl.bbstacz.actions.TaczFireActionClip;
import mcaxolotl.bbstacz.actions.TaczProjectileActionClip;
import mcaxolotl.bbstacz.network.RecordCursorSync;
import mchorse.bbs_mod.actions.ActionRecorder;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.film.Film;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ActionRecorder.class,remap=false)
public abstract class TaczActionTimingMixin {
    @Shadow private Film film;
    @Shadow private ServerPlayer entity;
    @Shadow private int countdown;
    @Shadow private int initialTick;
    @Inject(method="<init>",at=@At("TAIL"))
    private void bbstacz$newTake(Film film,ServerPlayer player,int tick,int countdown,CallbackInfo ci) { RecordCursorSync.reset(player); }
    @Inject(method="add",at=@At("RETURN"))
    private void bbstacz$poseCursor(ActionClip clip,CallbackInfo ci) {
        if(countdown>0 || !(clip instanceof TaczFireActionClip || clip instanceof TaczProjectileActionClip)) return;
        Integer time=RecordCursorSync.time(entity,film.getId()); if(time!=null) clip.tick.set(Math.max(initialTick,time));
    }
}
