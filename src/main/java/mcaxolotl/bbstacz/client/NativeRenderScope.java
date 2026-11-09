package mcaxolotl.bbstacz.client;

import java.util.ArrayDeque;
import java.util.Map;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.client.particle.ThirdPersonMuzzleParticleManager;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.client.forms.TaczVisualPlayer;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.PartPose;
import mcaxolotl.bbstacz.recording.ReplayBindings;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.minecraft.world.entity.LivingEntity;

/** Actor identity and evaluated pose for one native-player draw or read-only matrix evaluation. */
public final class NativeRenderScope implements AutoCloseable {
    private static final ThreadLocal<ArrayDeque<NativeRenderScope>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);
    public final TaczForm form;
    public final IEntity entity;
    public final TaczVisualPlayer player;
    public final float delta;
    public final FrameSnapshot frame;
    public final Map<String, PartPose> pose;
    public final boolean matrices;
    private final LivingEntity previousEntity;
    private final boolean previousMuzzle, previousShell;
    private boolean closed;
    private final com.tacz.guns.client.animation.third.AnimationRenderContext animation;
    private final Runnable restorePlayerAnimation;
    private final Long previousFlash;
    private final float previousRotation;
    public NativeRenderScope(TaczForm form, IEntity entity, TaczVisualPlayer player, float delta, Map<String, PartPose> pose, boolean matrices) {
        this.form = form; this.entity = entity; this.player = player; this.delta = delta; this.pose = pose; this.matrices = matrices;
        frame = ReplayBindings.sample(entity, delta);
        previousEntity = ThirdPersonMuzzleParticleManager.currentEntity; previousMuzzle = MuzzleFlashRender.isSelf; previousShell = ShellRender.isSelf;
        ThirdPersonMuzzleParticleManager.currentEntity = matrices || com.tacz.guns.compat.iris.IrisCompat.isRenderShadow() ? null : player;
        previousFlash = mcaxolotl.bbstacz.mixin.client.MuzzleFlashAccess.bbstacz$thirdPersonTimes().get(player);
        previousRotation = mcaxolotl.bbstacz.mixin.client.MuzzleFlashAccess.bbstacz$rotation();
        if (!matrices) {
            Long timestamp = null;
            if (frame != null && frame.shotId() >= 0) timestamp = com.tacz.guns.util.time.GunTime.nowMillis() - (long) frame.shotAgeMillis();
            else if (entity instanceof mchorse.bbs_mod.forms.entities.MCEntity mc && mc.getMcEntity() instanceof LivingEntity live)
                timestamp = mcaxolotl.bbstacz.mixin.client.MuzzleFlashAccess.bbstacz$thirdPersonTimes().get(live);
            if (timestamp != null) mcaxolotl.bbstacz.mixin.client.MuzzleFlashAccess.bbstacz$thirdPersonTimes().put(player, timestamp);
            if (frame != null) mcaxolotl.bbstacz.mixin.client.MuzzleFlashAccess.bbstacz$rotation(frame.flashRotation());
        }
        // Native PA body/limb transforms are evaluated once and applied below as a baked pose.
        // Suppress a second application from the visual player's own layers during this draw.
        animation = com.tacz.guns.client.animation.third.AnimationRenderContext.hand(player);
        restorePlayerAnimation = EvaluatedPlayerAnimation.bind(player, pose);
        MuzzleFlashRender.isSelf = false; ShellRender.isSelf = false; SCOPES.get().push(this);
    }
    public static NativeRenderScope current() { return SCOPES.get().peek(); }
    @Override public void close() {
        if (closed) return; closed = true;
        ThirdPersonMuzzleParticleManager.currentEntity = previousEntity; MuzzleFlashRender.isSelf = previousMuzzle; ShellRender.isSelf = previousShell;
        if (previousFlash == null) mcaxolotl.bbstacz.mixin.client.MuzzleFlashAccess.bbstacz$thirdPersonTimes().remove(player);
        else mcaxolotl.bbstacz.mixin.client.MuzzleFlashAccess.bbstacz$thirdPersonTimes().put(player, previousFlash);
        mcaxolotl.bbstacz.mixin.client.MuzzleFlashAccess.bbstacz$rotation(previousRotation);
        restorePlayerAnimation.run(); animation.close(); SCOPES.get().remove(this);
    }
    public static void clear() { while (current() != null) current().close(); SCOPES.remove(); }
}
