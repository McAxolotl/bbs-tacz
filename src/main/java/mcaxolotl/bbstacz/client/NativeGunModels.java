package mcaxolotl.bbstacz.client;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.BedrockGunModel;
import java.util.IdentityHashMap;
import java.util.Map;
import mcaxolotl.bbstacz.recording.PartPose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/** Restore shared native gun bones/visibility/camera after a form's held and holstered item layers draw. */
public final class NativeGunModels implements AutoCloseable {
    private record State(Map<String, PartPose> bones, Quaternionf camera, boolean hands) {}
    private final Map<BedrockGunModel, State> models = new IdentityHashMap<>();
    public NativeGunModels(Player player) {
        capture(player.getMainHandItem()); capture(player.getOffhandItem());
        for (int i=0; i<9; i++) capture(player.getInventory().getItem(i));
    }
    private void capture(ItemStack stack) {
        TimelessAPI.getGunDisplay(stack).ifPresent(display -> {
            capture(display.getGunModel());
            var lod = display.getLodModel(); if (lod != null) capture(lod.getLeft());
        });
    }
    private void capture(BedrockGunModel model) {
        if (model != null && !models.containsKey(model)) models.put(model, new State(NativePose.capture(model), new Quaternionf(model.getCameraAnimationObject().rotationQuaternion), model.getRenderHand()));
    }
    @Override public void close() {
        models.forEach((model, state) -> { NativePose.apply(model, state.bones); model.getCameraAnimationObject().rotationQuaternion.set(state.camera); model.setRenderHand(state.hands); });
    }
}
