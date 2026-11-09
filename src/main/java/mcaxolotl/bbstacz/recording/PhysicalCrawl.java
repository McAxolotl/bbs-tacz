package mcaxolotl.bbstacz.recording;

import cn.sh1rocu.tacz.api.mixin.ForcePoseInjection;
import com.tacz.guns.api.entity.IGunOperator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/** Restores an already validated recorded state. It does not pretend dry crawling is water swimming. */
public final class PhysicalCrawl {
    private record Previous(Pose pose, Pose forced, boolean crawling, boolean swimming) {}
    private static final Map<LivingEntity, Previous> OWNED = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private PhysicalCrawl() {}
    public static boolean isCrawling(LivingEntity entity) {
        var data = IGunOperator.fromLivingEntity(entity).getDataHolder();
        // Player dimensions/eye height are queried from Entity's constructor, before TACZ initializes its holder.
        return data != null && data.isCrawling
            || entity.getPose() == Pose.SWIMMING && !entity.isSwimming() && !entity.isInWater();
    }
    public static void apply(LivingEntity entity, boolean crawling) {
        if (IGunOperator.fromLivingEntity(entity).getDataHolder() == null) return;
        OWNED.computeIfAbsent(entity, e -> new Previous(e.getPose(),
            e instanceof ForcePoseInjection forced ? forced.tacz$getForcedPose() : null,
            IGunOperator.fromLivingEntity(e).getDataHolder().isCrawling, e.isSwimming()));
        IGunOperator.fromLivingEntity(entity).getDataHolder().isCrawling = crawling;
        if (entity instanceof ForcePoseInjection forced) forced.tacz$setForcedPose(crawling ? Pose.SWIMMING : null);
        if (crawling) {
            entity.setSwimming(false);
            entity.setPose(Pose.SWIMMING);
        } else if (entity.getPose() == Pose.SWIMMING && !entity.isInWater()) {
            entity.setPose(entity.isShiftKeyDown() ? Pose.CROUCHING : Pose.STANDING);
        }
        entity.refreshDimensions();
    }
    public static boolean isManaged(LivingEntity entity) { return OWNED.containsKey(entity); }
    public static void release(LivingEntity entity) {
        Previous original = OWNED.remove(entity);
        if (original == null) return;
        IGunOperator.fromLivingEntity(entity).getDataHolder().isCrawling = original.crawling;
        if (entity instanceof ForcePoseInjection forced) forced.tacz$setForcedPose(original.forced);
        entity.setSwimming(original.swimming); entity.setPose(original.pose);
        entity.refreshDimensions();
    }
    public static void clearClient() { clearSide(true); }
    public static void clearServer() { clearSide(false); }
    private static void clearSide(boolean client) {
        LivingEntity[] entities;
        synchronized (OWNED) { entities = OWNED.keySet().toArray(LivingEntity[]::new); }
        for (LivingEntity entity : entities) if (entity.level().isClientSide == client) release(entity);
    }
}
