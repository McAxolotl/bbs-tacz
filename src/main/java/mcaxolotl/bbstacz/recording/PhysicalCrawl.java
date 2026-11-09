package mcaxolotl.bbstacz.recording;

import cn.sh1rocu.tacz.api.mixin.ForcePoseInjection;
import com.tacz.guns.api.entity.IGunOperator;
import java.util.Objects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/** Validated recorded crawling with independent actor/camera owners and one original-state restore point. */
public final class PhysicalCrawl {
    private record Previous(Pose pose, Pose forced, boolean crawling, boolean swimming) {}
    private record Request(boolean crawling, long order) {}
    private static final class State {
        final Previous original;
        final WeakIdentityMap<Object, Request> requests = new WeakIdentityMap<>();
        State(LivingEntity entity) { original = new Previous(entity.getPose(), entity instanceof ForcePoseInjection forced ? forced.tacz$getForcedPose() : null,
            IGunOperator.fromLivingEntity(entity).getDataHolder().isCrawling, entity.isSwimming()); }
    }
    private static final WeakIdentityMap<LivingEntity, State> OWNED = new WeakIdentityMap<>();
    private static final Object LEGACY_OWNER = new Object();
    private static long sequence;
    private PhysicalCrawl() {}
    public static boolean isCrawling(LivingEntity entity) {
        var data = IGunOperator.fromLivingEntity(entity).getDataHolder();
        return data != null && data.isCrawling || entity.getPose() == Pose.SWIMMING && !entity.isSwimming() && !entity.isInWater();
    }
    public static void apply(LivingEntity entity, boolean crawling) { apply(entity, LEGACY_OWNER, crawling); }
    public static void apply(LivingEntity entity, Object owner, boolean crawling) {
        Objects.requireNonNull(owner, "Crawl owner");
        synchronized (OWNED) {
            if (IGunOperator.fromLivingEntity(entity).getDataHolder() == null) return;
            State state = OWNED.computeIfAbsent(entity, State::new);
            state.requests.put(owner, new Request(crawling, ++sequence)); set(entity, crawling);
        }
    }
    private static void set(LivingEntity entity, boolean crawling) {
        IGunOperator.fromLivingEntity(entity).getDataHolder().isCrawling = crawling;
        if (entity instanceof ForcePoseInjection forced) forced.tacz$setForcedPose(crawling ? Pose.SWIMMING : null);
        if (crawling) { entity.setSwimming(false); entity.setPose(Pose.SWIMMING); }
        else if (entity.getPose() == Pose.SWIMMING && !entity.isInWater()) entity.setPose(entity.isShiftKeyDown() ? Pose.CROUCHING : Pose.STANDING);
        entity.refreshDimensions();
    }
    public static boolean isManaged(LivingEntity entity) {
        synchronized (OWNED) {
            State state = OWNED.get(entity);
            if (state == null) return false;
            if (state.requests.isEmpty()) { OWNED.remove(entity); restore(entity, state.original); return false; }
            return true;
        }
    }
    public static void release(LivingEntity entity, Object owner) {
        synchronized (OWNED) {
            State state = OWNED.get(entity); if (state == null) return;
            state.requests.remove(owner);
            var latest = state.requests.values().stream().max(java.util.Comparator.comparingLong(Request::order)).orElse(null);
            if (latest != null) set(entity, latest.crawling);
            else { OWNED.remove(entity); restore(entity, state.original); }
        }
    }
    /** Whole-state release is reserved for explicit cleanup/disconnect and the legacy API. */
    public static void release(LivingEntity entity) {
        synchronized (OWNED) { State state = OWNED.remove(entity); if (state != null) restore(entity, state.original); }
    }
    private static void restore(LivingEntity entity, Previous original) {
        var data = IGunOperator.fromLivingEntity(entity).getDataHolder(); if (data != null) data.isCrawling = original.crawling;
        if (entity instanceof ForcePoseInjection forced) forced.tacz$setForcedPose(original.forced);
        entity.setSwimming(original.swimming); entity.setPose(original.pose); entity.refreshDimensions();
    }
    public static void clearClient() { clearSide(true); }
    public static void clearServer() { clearSide(false); }
    private static void clearSide(boolean client) {
        LivingEntity[] entities;
        synchronized (OWNED) { entities = OWNED.keys().toArray(LivingEntity[]::new); }
        for (LivingEntity entity : entities) if (entity.level().isClientSide == client) release(entity);
    }
}
