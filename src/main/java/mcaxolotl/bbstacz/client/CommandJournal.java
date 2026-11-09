package mcaxolotl.bbstacz.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.TimelessAPI;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Recorder;
import net.minecraft.client.Minecraft;

/** Ordered low-level commands are saved alongside the baked pose, including multiple operations in one tick. */
public final class CommandJournal {
    private static final Gson GSON = new Gson();
    private static Recorder owner;
    private static long sequence;
    private CommandJournal() {}
    public static Recorder recorder() {
        if (BBSModClient.getFilms() == null) return null;
        Recorder recorder = BBSModClient.getFilms().getRecorder();
        return recorder != null && !recorder.hasNotStarted() && recorder.getTick() >= 0 && !Minecraft.getInstance().isPaused() ? recorder : null;
    }
    public static float time(Recorder recorder) {
        return Math.max(recorder.initialTick, recorder.getTick() - 1 + Minecraft.getInstance().getFrameTime());
    }
    public static boolean recording(AnimationStateMachine<?> machine) {
        if (recorder() == null) return false;
        var stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        if (TimelessAPI.getGunDisplay(stack).map(display -> display.getAnimationStateMachine() == machine).orElse(false)) return true;
        var player = Minecraft.getInstance().player;
        return player != null && TimelessAPI.getGunDisplay(player.getMainHandItem()).map(display -> display.getAnimationStateMachine() == machine).orElse(false);
    }
    public static void capture(String operation, Object... arguments) {
        Recorder recorder = recorder();
        if (recorder == null || FirstPersonPlayback.isRendering()) return;
        if (owner != recorder) { owner = recorder; sequence = 0; }
        var channel = ((TaczReplayChannels) recorder.keyframes).bbstacz$commands();
        float tick = time(recorder);
        JsonArray batch = new JsonArray();
        var keys = channel.getKeyframes();
        for (int i = keys.size() - 1; i >= 0; i--) {
            var key = keys.get(i);
            if (key.getTick() == tick) { batch = JsonParser.parseString(key.getValue()).getAsJsonArray(); break; }
            if (key.getTick() < tick) break;
        }
        JsonObject command = new JsonObject();
        command.addProperty("v", 1); command.addProperty("seq", ++sequence); command.addProperty("op", operation);
        command.add("args", GSON.toJsonTree(arguments)); batch.add(command);
        channel.insert(tick, GSON.toJson(batch));
    }
    public static void clear() { owner = null; sequence = 0; }
}
