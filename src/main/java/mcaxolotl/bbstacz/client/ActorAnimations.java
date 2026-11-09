package mcaxolotl.bbstacz.client;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.compat.playeranimator.AnimationName;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import com.tacz.guns.compat.playeranimator.animation.AnimationManager;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import mcaxolotl.bbstacz.client.forms.TaczVisualPlayer;
import mcaxolotl.bbstacz.recording.ReplayBindings;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;

/** A live non-player actor owns one native PA session. Film actors use baked poses, never a second clock. */
public final class ActorAnimations {
    private static final Map<LivingEntity, List<WeakReference<TaczVisualPlayer>>> OWNERS = new WeakHashMap<>();
    private static final Map<TaczVisualPlayer, Long> LAST_TICK = new WeakHashMap<>();
    private ActorAnimations() {}
    public static void tick(IEntity source, TaczVisualPlayer proxy) {
        if (!PlayerAnimatorCompat.isInstalled()) return;
        boolean recorded = ReplayBindings.get(source) != null;
        if (recorded || source instanceof MCEntity mc && mc.getMcEntity() instanceof AbstractClientPlayer) {
            LAST_TICK.remove(proxy); AnimationManager.stopAllAnimation(proxy, 0); return;
        }
        if (source instanceof MCEntity mc && mc.getMcEntity() instanceof LivingEntity owner) {
            var list = OWNERS.computeIfAbsent(owner, e -> new ArrayList<>());
            list.removeIf(reference -> reference.get() == null);
            if (list.stream().noneMatch(reference -> reference.get() == proxy)) list.add(new WeakReference<>(proxy));
        }
        long tick = proxy.level().getGameTime();
        Long previous = LAST_TICK.put(proxy, tick); if (previous != null && previous == tick) return;
        var display = TimelessAPI.getGunDisplay(proxy.getMainHandItem()).orElse(null);
        if (display == null || !PlayerAnimatorCompat.canPlayAnimation(proxy)) AnimationManager.stopAllAnimation(proxy);
        else PlayerAnimatorCompat.playAnimation(proxy, display, proxy.walkAnimation.speed());
        ((IAnimatedPlayer) (Object) proxy).playerAnimator_getAnimation().tick();
    }
    public static boolean owns(IEntity source) {
        return ReplayBindings.get(source) == null && !(source instanceof MCEntity mc && mc.getMcEntity() instanceof AbstractClientPlayer);
    }
    /** Direct native animation API; never reposts events (which would also duplicate sounds/projectiles). */
    public static void event(LivingEntity owner, ItemStack stack, String operation) {
        var proxies = OWNERS.get(owner); if (proxies == null) return;
        var display = TimelessAPI.getGunDisplay(stack).orElse(null); if (display == null) return;
        proxies.removeIf(reference -> reference.get() == null);
        for (var reference : proxies) {
            var player = reference.get(); if (player == null || !LAST_TICK.containsKey(player)) continue;
            boolean lie = player.getPose() == Pose.SWIMMING && !player.isSwimming();
            boolean aim = com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(player).getSynAimingProgress() > 0;
            String name = switch (operation) {
                case "reload" -> lie ? AnimationName.LIE_RELOAD : AnimationName.RELOAD_UPPER;
                case "melee" -> switch (owner.getRandom().nextInt(3)) { case 0 -> AnimationName.MELEE_UPPER; case 1 -> AnimationName.MELEE_2_UPPER; default -> AnimationName.MELEE_3_UPPER; };
                case "fire" -> lie ? aim ? AnimationName.LIE_AIM_FIRE : AnimationName.LIE_NORMAL_FIRE : aim ? AnimationName.AIM_FIRE_UPPER : AnimationName.NORMAL_FIRE_UPPER;
                default -> null;
            };
            if (name != null) AnimationManager.playOnceAnimation(player, display, PlayerAnimatorCompat.ONCE_UPPER_ANIMATION, name);
            else if (operation.equals("draw")) AnimationManager.stopAllAnimation(player);
        }
    }
    public static void clear() { OWNERS.clear(); LAST_TICK.clear(); }
}
