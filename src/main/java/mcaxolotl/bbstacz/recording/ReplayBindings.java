package mcaxolotl.bbstacz.recording;

import java.util.Map;
import java.util.WeakHashMap;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.world.entity.LivingEntity;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.film.replays.Replay;

public final class ReplayBindings {
    private static final Map<IEntity, Binding> BINDINGS = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    public record Binding(KeyframeChannel<FrameSnapshot> channel, float tick, ReplayKeyframes keyframes, Replay replay) {
        public FrameSnapshot sample(float delta) { return FrameSampler.sample(channel, tick + delta); }
    }
    private ReplayBindings() {}
    public static void bind(IEntity entity, KeyframeChannel<FrameSnapshot> channel, float tick) {
        var parent = channel.getParent();
        ReplayKeyframes keyframes = parent instanceof ReplayKeyframes k ? k : null;
        Replay replay = keyframes != null && keyframes.getParent() instanceof Replay r ? r : null;
        if (channel.isEmpty()) { release(entity); return; }
        BINDINGS.put(entity, new Binding(channel, tick, keyframes, replay));
        FrameSnapshot frame = FrameSampler.sample(channel, tick);
        entity.setLeaningPitch(frame.crawling() || entity.isSwimming() ? 1 : 0);
        if (entity instanceof MCEntity mc && mc.getMcEntity() instanceof LivingEntity living) PhysicalCrawl.apply(living, frame.crawling());
    }
    public static FrameSnapshot sample(IEntity entity, float delta) {
        Binding binding = BINDINGS.get(entity);
        return binding == null ? null : binding.sample(delta);
    }
    public static Binding get(IEntity entity) { return BINDINGS.get(entity); }
    public static Map<IEntity, Binding> snapshot() { synchronized (BINDINGS) { return Map.copyOf(BINDINGS); } }
    public static void release(IEntity entity) {
        BINDINGS.remove(entity); entity.setLeaningPitch(entity.isSwimming() ? 1 : 0);
        if (entity instanceof MCEntity mc && mc.getMcEntity() instanceof LivingEntity living) PhysicalCrawl.release(living);
    }
    public static void clear() { BINDINGS.clear(); }
}
