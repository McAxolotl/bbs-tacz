package mcaxolotl.bbstacz.smoke.mixin;

import com.tacz.guns.util.InputExtraCheck;
import mcaxolotl.bbstacz.smoke.ReviewClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Test-only desktop-focus adapter. GUI/loading restrictions and every gameplay precheck remain native.
 * A user reading test output in another window must not turn an integration scenario into a false gun failure. */
@Mixin(value=InputExtraCheck.class,remap=false)
public abstract class ReviewInputFocusMixin {
    @Inject(method="isInGame",at=@At("RETURN"),cancellable=true)
    private static void bbstacz$reviewFocus(CallbackInfoReturnable<Boolean> ci) {
        if(!ReviewClient.isRunning()) return;
        var client=Minecraft.getInstance();
        ci.setReturnValue(client.player!=null&&client.level!=null&&client.screen==null&&client.getOverlay()==null);
    }
}
