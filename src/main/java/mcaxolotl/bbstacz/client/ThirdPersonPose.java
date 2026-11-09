package mcaxolotl.bbstacz.client;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.third.InnerThirdPersonManager;
import java.util.LinkedHashMap;
import java.util.Map;
import mcaxolotl.bbstacz.recording.PartPose;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class ThirdPersonPose {
    private static PlayerModel<LivingEntity> model;
    private static PlayerModel<LivingEntity> slimModel;
    private static boolean sampling;
    public static boolean isSampling() { return sampling; }
    private ThirdPersonPose() {}
    public static Map<String, PartPose> sample(LivingEntity entity, float delta) {
        return sample(entity, delta, entity instanceof AbstractClientPlayer player && player.getModelName().equals("slim"));
    }
    public static Map<String, PartPose> sample(LivingEntity entity, float delta, boolean slim) {
        if (sampling) return Map.of();
        if (model == null) model = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        if (slimModel == null) slimModel = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        PlayerModel<LivingEntity> model = slim ? slimModel : ThirdPersonPose.model;
        sampling = true;
        boolean firstPersonPass = dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode.isFirstPersonPass();
        dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode.setFirstPersonPass(false);
        try {
            model.head.resetPose(); model.body.resetPose(); model.leftArm.resetPose(); model.rightArm.resetPose(); model.leftLeg.resetPose(); model.rightLeg.resetPose();
            if (entity instanceof AbstractClientPlayer player && FabricLoader.getInstance().isModLoaded("player-animator")) PlayerAnimatorBridge.prepare(player, delta);
            float limb = entity.walkAnimation.position(delta), speed = entity.walkAnimation.speed(delta);
            float yaw = net.minecraft.util.Mth.rotLerp(delta, entity.yHeadRotO, entity.yHeadRot)
                - net.minecraft.util.Mth.rotLerp(delta, entity.yBodyRotO, entity.yBodyRot);
            model.attackTime = entity.getAttackAnim(delta);
            model.riding = entity.isPassenger(); model.young = entity.isBaby(); model.crouching = entity.isCrouching();
            model.prepareMobModel(entity, limb, speed, delta);
            model.setupAnim(entity, limb, speed, entity.tickCount + delta, yaw, entity.getViewXRot(delta));
            Map<String, PartPose> out = new LinkedHashMap<>();
            out.put("motion", new PartPose(new float[]{entity.getSwimAmount(delta),0,0,0,0,0,0,0,0,1,1,1,0,0,0,1},true,false));
            out.put("head", NativePose.capture(model.head)); out.put("torso", NativePose.capture(model.body));
            out.put("right_arm", NativePose.capture(model.rightArm)); out.put("left_arm", NativePose.capture(model.leftArm));
            out.put("right_leg", NativePose.capture(model.rightLeg)); out.put("left_leg", NativePose.capture(model.leftLeg));
            if (entity instanceof AbstractClientPlayer player && FabricLoader.getInstance().isModLoaded("player-animator")) PlayerAnimatorBridge.captureBody(player, out);
            return out;
        } finally { sampling = false; dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode.setFirstPersonPass(firstPersonPass); }
    }
    public static void clear() { model = null; slimModel = null; }
}
