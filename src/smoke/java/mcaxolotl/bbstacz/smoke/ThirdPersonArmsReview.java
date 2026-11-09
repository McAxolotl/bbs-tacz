package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import java.util.ArrayList;
import mcaxolotl.bbstacz.client.ThirdPersonPose;
import mcaxolotl.bbstacz.client.forms.TaczFormRenderer;
import mcaxolotl.bbstacz.forms.TaczForm;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.StubEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/** Reproduce unticked editor/manual-replay forms instead of pre-initializing the proxy with renderer.tick(). */
public final class ThirdPersonArmsReview implements ReviewClient.Scenario {
    private StubEntity source;
    private TaczFormRenderer renderer;
    private float observed;
    private int draws;
    @Override public boolean tick(Minecraft client, int tick) throws Exception {
        if (tick == 0) {
            var gun = GunItemBuilder.create().setId(new ResourceLocation("tacz", "ak47")).setAmmoCount(30).setAmmoInBarrel(true).build();
            client.player.setItemSlot(EquipmentSlot.MAINHAND, gun); AttachmentPropertyManager.postChangeEvent(client.player, gun);
            source = new StubEntity(client.level); source.setOnGround(true); source.setEquipmentStack(EquipmentSlot.MAINHAND, gun); source.setHotbarStack(0, gun);
            TaczForm form = new TaczForm(); source.setForm(form); renderer = (TaczFormRenderer) FormUtilsClient.getRenderer(form);
            ReviewProbe.playerDraw = pose -> { if (pose.scope().entity == source) { observed = pose.rightX(); draws++; } };
        }
        if (tick >= 25 && tick < 42) NativePreviewSmoke.render(client, renderer, source, "review-arms-unticked.png", false);
        if (tick == 42) {
            var reference = ThirdPersonPose.sample(client.player, 0, false).get("right_arm");
            var proxy = renderer.visualPlayer(); var animation = ((dev.kosmx.playerAnim.impl.IAnimatedPlayer)(Object)proxy).playerAnimator_getAnimation();
            System.out.println("[REVIEW arms] proxy gun="+proxy.getMainHandItem()+" active="+animation.isActive()+" owns="+mcaxolotl.bbstacz.client.ActorAnimations.owns(source)+" handScope="+com.tacz.guns.client.animation.third.AnimationRenderContext.isFirstPersonHand(proxy)+" game="+proxy.level().getGameTime()+" raw="+animation.get3DTransform("rightArm",dev.kosmx.playerAnim.api.TransformType.ROTATION,dev.kosmx.playerAnim.core.util.Vec3f.ZERO));
            System.out.println("[REVIEW arms] proxy sampled="+ThirdPersonPose.sample(proxy,0,false).get("right_arm").at(3));
            System.out.println("[REVIEW arms] unticked actual=" + observed + " native=" + reference.at(3) + " draws=" + draws);
            ReviewClient.require(draws > 0 && Math.abs(reference.at(3)) > 0.6, "Native reference is not holding the test gun");
            ReviewClient.require(Math.abs(observed) > 0.6, "Unticked TACZ form arm remains down: actual=" + observed + " native=" + reference.at(3));
            var replay = new mchorse.bbs_mod.film.replays.Replay("empty_pose");
            ((mcaxolotl.bbstacz.recording.TaczReplayChannels)replay.keyframes).bbstacz$state().insert(0, mcaxolotl.bbstacz.recording.FrameSnapshot.EMPTY);
            replay.keyframes.hotbar.get(0).insert(0, client.player.getMainHandItem()); replay.keyframes.apply(0, source);
        }
        if (tick >= 43 && tick < 60) NativePreviewSmoke.render(client, renderer, source, "review-arms-empty-pose.png", false);
        if (tick == 60) {
            ReviewClient.require(Math.abs(observed) > 0.6, "Empty recorded pose incorrectly disables native gun-hold animation");
            ReviewProbe.playerDraw = null; mcaxolotl.bbstacz.recording.ReplayBindings.release(source); return true;
        }
        return false;
    }
}
