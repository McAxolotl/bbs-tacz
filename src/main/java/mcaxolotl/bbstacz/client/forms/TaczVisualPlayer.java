package mcaxolotl.bbstacz.client.forms;

import com.mojang.authlib.GameProfile;
import cn.sh1rocu.tacz.api.mixin.ForcePoseInjection;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.PhysicalCrawl;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.mixin.EntityInvoker;
import mchorse.bbs_mod.mixin.LimbAnimatorAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

/** A render-only player; it never joins the world or emits real gameplay actions. */
public final class TaczVisualPlayer extends RemotePlayer {
    public boolean slim;
    private float lean, previousLean;
    private ResourceLocation skin, selectedSkin;
    public void selectedSkin(ResourceLocation skin) { selectedSkin = skin; }
    public TaczVisualPlayer(ClientLevel level, GameProfile profile, boolean slim) { super(level, profile); this.slim = slim; noPhysics = true; }
    @Override public String getModelName() { return slim ? "slim" : "default"; }
    @Override public ResourceLocation getSkinTextureLocation() {
        return selectedSkin != null ? selectedSkin : skin != null ? skin : new ResourceLocation("minecraft", "textures/entity/player/" + (slim ? "slim/alex.png" : "wide/steve.png"));
    }
    @Override public float getSwimAmount(float delta) { return previousLean + (lean - previousLean) * delta; }
    public void sync(IEntity source, FrameSnapshot frame) { sync(source, frame, 0); }
    public void sync(IEntity source, FrameSnapshot frame, float delta) {
        if (source instanceof MCEntity mc && mc.getMcEntity() instanceof AbstractClientPlayer player) skin = player.getSkinTextureLocation();
        else skin = null;
        // Real ActorEntity has no nine-slot inventory. Read the recorded hotbar directly instead of losing holstered guns.
        var binding = mcaxolotl.bbstacz.recording.ReplayBindings.get(source);
        var equipment = frame == null || binding == null ? null : binding.keyframes();
        float tick = binding == null ? 0 : binding.tick() + delta;
        getInventory().selected = equipment != null && !equipment.selectedSlot.isEmpty() ? equipment.getSelectedSlot(tick) : Math.max(0, Math.min(8, source.getSelectedSlot()));
        for (int i = 0; i < 9; i++) getInventory().setItem(i, (equipment != null && !equipment.hotbar.get(i).isEmpty()
            ? equipment.hotbar.get(i).interpolate(tick, net.minecraft.world.item.ItemStack.EMPTY) : source.getHotbarStack(i)).copy());
        if (equipment == null && !source.isMainHandInHotbar()) setItemSlot(EquipmentSlot.MAINHAND, source.getEquipmentStack(EquipmentSlot.MAINHAND).copy());
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var channel = equipment == null ? null : equipment.getEquipmentChannel(slot);
            setItemSlot(slot, (channel != null && !channel.isEmpty() ? channel.interpolate(tick, net.minecraft.world.item.ItemStack.EMPTY) : source.getEquipmentStack(slot)).copy());
        }
        setPos(source.getX(), source.getY(), source.getZ()); xo = xOld = source.getPrevX(); yo = yOld = source.getPrevY(); zo = zOld = source.getPrevZ();
        setYRot(0); yRotO = 0; yBodyRot = yBodyRotO = 0;
        yHeadRot = source.getHeadYaw() - source.getBodyYaw(); yHeadRotO = source.getPrevHeadYaw() - source.getPrevBodyYaw();
        setXRot(source.getPitch()); xRotO = source.getPrevPitch();
        setOnGround(source.isOnGround()); setShiftKeyDown(source.isSneaking()); setSprinting(source.isSprinting()); setSwimming(source.isSwimming());
        ((EntityInvoker) (Object) this).bbs$setFlag(7, source.isFallFlying());
        boolean crawl = frame != null ? frame.crawling() : source instanceof MCEntity mc && mc.getMcEntity() instanceof net.minecraft.world.entity.LivingEntity l && PhysicalCrawl.isCrawling(l);
        ((ForcePoseInjection) (Object) this).tacz$setForcedPose(crawl ? Pose.SWIMMING : null);
        setPose(crawl ? Pose.SWIMMING : source.getEntityPose());
        tickCount = source.getAge();
        previousLean = source.getLeaningPitch(0); lean = source.getLeaningPitch(1);
        if (frame != null && frame.humanoid().containsKey("motion")) lean = previousLean = frame.humanoid().get("motion").at(0);
        else if (crawl && frame != null) lean = previousLean = 1;
        if (walkAnimation instanceof LimbAnimatorAccessor a && source.getLimbAnimator() instanceof LimbAnimatorAccessor b) {
            a.setPrevSpeed(b.getPrevSpeed()); a.setSpeed(b.getSpeed()); a.setPos(b.getPos());
        }
        attackAnim = source.getHandSwingProgress(1); oAttackAnim = source.getHandSwingProgress(0);
        var velocity = source.getVelocity(); setDeltaMovement(velocity.x, velocity.y, velocity.z);
        getAbilities().flying = source.isFlying();
        var data = IGunOperator.fromLivingEntity(this).getDataHolder();
        data.setCurrentGunItem(this::getMainHandItem); data.isCrawling = crawl;
        float aim = frame != null ? frame.aiming() : source instanceof MCEntity mc && mc.getMcEntity() instanceof net.minecraft.world.entity.LivingEntity l ? IGunOperator.fromLivingEntity(l).getSynAimingProgress() : 0;
        data.aimingProgress = aim; data.isAiming = aim > 0;
        ModSyncedEntityData.AIMING_PROGRESS_KEY.setValue(this, aim); ModSyncedEntityData.IS_AIMING_KEY.setValue(this, aim > 0);
        if (source instanceof MCEntity mc && mc.getMcEntity() instanceof net.minecraft.world.entity.LivingEntity l) {
            var op = IGunOperator.fromLivingEntity(l);
            ModSyncedEntityData.RELOAD_STATE_KEY.setValue(this, op.getSynReloadState());
            ModSyncedEntityData.SPRINT_TIME_KEY.setValue(this, op.getSynSprintTime());
            ModSyncedEntityData.IS_BOLTING_KEY.setValue(this, op.getSynIsBolting());
        }
        // Vanilla renders all skin overlays when no player-list entry exists for a visual proxy.
        byte parts = source instanceof MCEntity mc && mc.getMcEntity() instanceof net.minecraft.world.entity.player.Player player
            ? player.getEntityData().get(mchorse.bbs_mod.utils.PlayerUtils.ProtectedAccess.getModelParts()) : (byte) 127;
        getEntityData().set(mchorse.bbs_mod.utils.PlayerUtils.ProtectedAccess.getModelParts(), parts);
        setInvisible(false); noPhysics = true;
    }
}
