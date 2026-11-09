package mcaxolotl.bbstacz.mixin;

import mchorse.bbs_mod.actions.ActionPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value=ActionPlayer.class,remap=false)
public interface ActionPlayerSeekAccess {
    @Invoker("applyAction") void bbstacz$applySeekActions();
}
