package mcaxolotl.bbstacz.client;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.util.time.GunTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mcaxolotl.bbstacz.recording.PoseMath;
import mcaxolotl.bbstacz.recording.ShellSnapshot;
import mchorse.bbs_mod.film.Recorder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/** Retain real Lua ejections across hotbar switches, independently of the currently rendered gun. */
public final class CapturedShells {
    private static final class Emission {
        final int origin; final ItemStack gun; final String nbt; final ShellRender.Data data;
        Matrix4f mirrorPose;
        Emission(int origin, ItemStack gun, String nbt, ShellRender.Data data) { this.origin=origin; this.gun=gun; this.nbt=nbt; this.data=data; }
    }
    private static final Map<Long, Emission> SHELLS = new LinkedHashMap<>();
    private static Recorder owner;
    private CapturedShells() {}
    private static boolean prepare() {
        Recorder recorder = CommandJournal.recorder();
        if (owner != recorder) { clear(); owner = recorder; }
        return recorder != null;
    }
    public static void emitted(int origin, ItemStack gun, ShellRender.Data data) {
        if (!prepare()) return;
        var copy = gun.copy();
        SHELLS.put(ShellIds.of(data), new Emission(origin, copy, copy.save(new CompoundTag()).toString(), data));
        while (SHELLS.size() > 128) SHELLS.remove(SHELLS.keySet().iterator().next());
    }
    /** F5 has no native ShellRender draw to initialize its emission matrix. Seed only our own copy, never the native queue. */
    public static void evaluated(com.tacz.guns.client.model.BedrockGunModel model, com.mojang.blaze3d.vertex.PoseStack pose) {
        if (!prepare()) return;
        for (var e : SHELLS.values()) {
            if (e.data.pose != null || e.mirrorPose != null || TimelessAPI.getGunDisplay(e.gun).map(d -> d.getGunModel() != model).orElse(true)) continue;
            var part = model.getNode(e.origin == 0 ? "shell" : "shell_" + e.origin); if (part == null) continue;
            var path = new ArrayList<com.tacz.guns.client.model.bedrock.BedrockPart>();
            for (var p = part; p != null; p = p.getParent()) path.add(0, p);
            pose.pushPose(); try { for (var p : path) p.translateAndRotateAndScale(pose); e.mirrorPose = new Matrix4f(pose.last().pose()); } finally { pose.popPose(); }
        }
    }
    public static List<ShellSnapshot> snapshot(Matrix4f inverse) {
        if (!prepare()) return List.of();
        long now = GunTime.nowMillis();
        var result = new ArrayList<ShellSnapshot>();
        SHELLS.entrySet().removeIf(entry -> {
            Emission e = entry.getValue(); var display = TimelessAPI.getGunDisplay(e.gun).orElse(null);
            if (e.data.epoch != GunTime.epoch() || display == null || display.getShellEjection() == null || now - e.data.timeStamp > display.getShellEjection().getLivingTime() * 1000) return true;
            float[] matrix = e.mirrorPose != null ? e.mirrorPose.get(new float[16]) : inverse == null || e.data.pose == null ? PoseMath.identity() : new Matrix4f(inverse).mul(e.data.pose).get(new float[16]);
            result.add(new ShellSnapshot(entry.getKey(), e.origin, now - e.data.timeStamp,
                new float[]{e.data.randomOffset.x, e.data.randomOffset.y, e.data.randomOffset.z}, matrix, e.nbt));
            return false;
        });
        return result;
    }
    public static void clear() { SHELLS.clear(); owner = null; }
}
