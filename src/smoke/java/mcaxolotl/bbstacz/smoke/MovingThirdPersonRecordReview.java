package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import java.util.Map;
import mcaxolotl.bbstacz.client.ThirdPersonPose;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.PartPose;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/** Compare native PA transforms and recorded body poses during actual walking/reload/melee, not stationary hold poses. */
public final class MovingThirdPersonRecordReview implements ReviewClient.Scenario {
    private Film film;
    private Map<String,PartPose> hold;
    private double startX,startZ;
    private int reloadEvents,meleeEvents;
    private float reloadDifference,meleeDifference;
    private int meleeTick=-1;
    private Map<String,PartPose> meleeStart;
    private Object reloadAnimation;
    private boolean replacedMelee;
    public MovingThirdPersonRecordReview() {
        com.tacz.guns.api.event.common.GunReloadEvent.CALLBACK.register(e->{if(e.getLogicalSide().isClient()) reloadEvents++;});
        com.tacz.guns.api.event.common.GunMeleeEvent.CALLBACK.register(e->{if(e.getLogicalSide().isClient()) meleeEvents++;});
    }
    private float difference(Map<String,PartPose> pose) {
        float result=0; if(hold==null) return 0;
        for(String role:new String[]{"right_arm","left_arm","torso"}) for(int i=3;i<9;i++)
            result=Math.max(result,Math.abs(pose.get(role).at(i)-hold.get(role).at(i)));
        return result;
    }
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        var operator=IClientPlayerGunOperator.fromLocalPlayer(client.player);
        if(tick==0) {
            var form=new TaczForm(); Morph.getMorph(client.player).setForm(form); client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            var gun=GunItemBuilder.create().setId(new ResourceLocation("tacz","ak47")).setAmmoCount(15).setAmmoInBarrel(true).build();
            client.player.setItemSlot(EquipmentSlot.MAINHAND,gun); AttachmentPropertyManager.postChangeEvent(client.player,gun);
            var uuid=client.player.getUUID(); client.getSingleplayerServer().submit(()-> {var p=client.getSingleplayerServer().getPlayerList().getPlayer(uuid); p.setItemSlot(EquipmentSlot.MAINHAND,gun.copy()); AttachmentPropertyManager.postChangeEvent(p,gun);}).get();
            film=new Film(); film.setId("bbs-tacz-moving-tp-"+System.currentTimeMillis()); var replay=new Replay("moving_tp"); replay.form.set(form); replay.fp.set(false); film.replays.add(replay);
        }
        if(tick==30) { BBSModClient.getFilms().startRecording(film,0,0,false); BBSModClient.getFilms().getRecorder().countdown=0; startX=client.player.getX(); startZ=client.player.getZ(); hold=ThirdPersonPose.sample(client.player,0); client.options.keyUp.setDown(true); }
        if(tick==45) operator.reload();
        if(tick>=70&&tick<170&&meleeTick<0&&!operator.getDataHolder().clientStateLock) {
            meleeStart=ThirdPersonPose.sample(client.player,0); operator.melee();
            if(meleeEvents>0) {
                meleeTick=tick; var once=(ModifierLayer<?>)PlayerAnimationAccess.getPlayerAssociatedData(client.player).get(PlayerAnimatorCompat.ONCE_UPPER_ANIMATION);
                replacedMelee=once.getAnimation()!=reloadAnimation;
                System.out.println("[REVIEW TP accepted melee] tick="+tick+" replaced reload="+replacedMelee);
            }
        }
        if(tick>50&&tick<68) reloadDifference=Math.max(reloadDifference,difference(ThirdPersonPose.sample(client.player,0)));
        if(meleeTick>=0&&tick>meleeTick+4&&tick<meleeTick+25) {
            var pose=ThirdPersonPose.sample(client.player,0); float change=0;
            for(String role:new String[]{"right_arm","left_arm","torso"}) for(int i=3;i<9;i++) change=Math.max(change,Math.abs(pose.get(role).at(i)-meleeStart.get(role).at(i)));
            meleeDifference=Math.max(meleeDifference,change);
        }
        if(tick==55) reloadAnimation=((ModifierLayer<?>)PlayerAnimationAccess.getPlayerAssociatedData(client.player).get(PlayerAnimatorCompat.ONCE_UPPER_ANIMATION)).getAnimation();
        if(tick==55||tick==80||tick==105) {
            var layer=(ModifierLayer<?>)PlayerAnimationAccess.getPlayerAssociatedData(client.player).get(PlayerAnimatorCompat.ONCE_UPPER_ANIMATION);
            Vec3f nativeArm=((IAnimatedPlayer)(Object)client.player).playerAnimator_getAnimation().get3DTransform("leftArm",TransformType.ROTATION,Vec3f.ZERO);
            System.out.println("[REVIEW TP dynamic] tick="+tick+" once="+(layer==null?null:layer.getAnimation())+" native arm="+nativeArm+" sampled arm="+ThirdPersonPose.sample(client.player,0).get("left_arm")+" events reload/melee="+reloadEvents+"/"+meleeEvents);
        }
        if(tick==200) {
            client.options.keyUp.setDown(false); var recorder=BBSModClient.getFilms().stopRecording(); var panel=BBSModClient.getDashboard().getPanel(UIFilmPanel.class); panel.fill(film); panel.applyRecordedKeyframes(recorder,film);
            double distance=Math.hypot(client.player.getX()-startX,client.player.getZ()-startZ);
            var frames=((TaczReplayChannels)film.replays.getList().get(0).keyframes).bbstacz$state();
            System.out.println("[REVIEW TP dynamic result] distance="+distance+" reload/melee pose difference="+reloadDifference+"/"+meleeDifference+" frames="+frames.getKeyframes().size()+" events="+reloadEvents+"/"+meleeEvents);
            ReviewClient.require(distance>1,"TP animation fixture did not actually walk");
            ReviewClient.require(reloadEvents>0&&meleeEvents>0,"Real native reload/melee input was not accepted");
            ReviewClient.require(replacedMelee,"Accepted native melee kept the previous reload animation instead of replacing it");
            ReviewClient.require(reloadDifference>0.05f&&meleeDifference>0.05f,"Native TP reload/melee was missing from moving body evaluation");
            var source=new mchorse.bbs_mod.forms.entities.StubEntity(client.level); source.setForm(new TaczForm());
            var renderer=(mcaxolotl.bbstacz.client.forms.TaczFormRenderer)mchorse.bbs_mod.forms.FormUtilsClient.getRenderer(source.getForm());
            for(int at:new int[]{25,35,Math.max(0,meleeTick-30)+12}) {
                var frame=mcaxolotl.bbstacz.recording.FrameSampler.sample(frames,at); film.replays.getList().get(0).keyframes.apply(at,source);
                final int sampleTick=at;
                ReviewProbe.playerDraw=draw->{if(draw.scope().entity==source) {
                    var expected=frame.humanoid().get("right_arm");
                    System.out.println("[REVIEW TP baked draw] tick="+sampleTick+" actual="+draw.rightX()+" expected="+expected.at(3));
                    ReviewClient.require(ReviewProbe.armError(draw,frame.humanoid())<0.03,"Third-person GPU draw lost the recorded moving animation");
                }};
                NativePreviewSmoke.render(client,renderer,source,"review-moving-tp-"+at+".png",false);
            }
            ReviewProbe.playerDraw=null; mcaxolotl.bbstacz.recording.ReplayBindings.release(source);
            return true;
        }
        return false;
    }
}
