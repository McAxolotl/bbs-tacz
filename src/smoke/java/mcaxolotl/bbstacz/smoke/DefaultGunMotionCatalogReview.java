package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import com.tacz.guns.compat.playeranimator.animation.AnimationManager;
import com.tacz.guns.compat.playeranimator.animation.PlayerAnimatorAssetManager;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import mcaxolotl.bbstacz.client.ThirdPersonPose;
import mcaxolotl.bbstacz.client.forms.TaczFormRenderer;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.FrameFactory;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.PartPose;
import mcaxolotl.bbstacz.recording.PhysicalCrawl;
import mcaxolotl.bbstacz.recording.PoseMath;
import mcaxolotl.bbstacz.recording.ReplayBindings;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.mixin.LimbAnimatorAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

/** Exhaustive loaded-gun x motion x declared TP animation native evaluation/codec/GPU catalog.
 * Logical animation frame sampling is controlled; this is not labelled as a live keyboard/server gameplay test. */
public final class DefaultGunMotionCatalogReview implements ReviewClient.Scenario {
    private static final String[] MOTIONS={"stand","walk","sprint","crouch_walk","crawl_move","airborne","ride","swim"};
    private final ArrayList<Map<String,Object>> rows=new ArrayList<>();
    private List<Map.Entry<ResourceLocation,com.tacz.guns.client.resource.index.ClientGunIndex>> guns;
    private Map<ResourceLocation,Map<String,KeyframeAnimation>> animations;
    private int gunIndex;
    @Override public int timeoutTicks() { return 14000; }
    @SuppressWarnings("unchecked")
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(guns==null) {
            guns=new ArrayList<>(TimelessAPI.getAllClientGunIndex()); guns.sort(Comparator.comparing(e->e.getKey().toString()));
            var field=PlayerAnimatorAssetManager.class.getDeclaredField("animations"); field.setAccessible(true);
            animations=(Map<ResourceLocation,Map<String,KeyframeAnimation>>)field.get(PlayerAnimatorAssetManager.get());
        }
        if(gunIndex>=guns.size()) {
            Path report=Path.of("bbs-tacz-motion-catalog.json"); Files.writeString(report,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));
            System.out.println("[REVIEW default catalog] guns="+guns.size()+" motion states="+MOTIONS.length+" rows="+rows.size()+" report="+report.toAbsolutePath()); return true;
        }
        var gunEntry=guns.get(gunIndex++); GunDisplayInstance display=gunEntry.getValue().getDefaultDisplay();
        var gun=GunItemBuilder.create().setId(gunEntry.getKey()).setAmmoCount(20).setAmmoInBarrel(true).build();
        var nativeForm=new TaczForm(); var owner=new ActorEntity(BBSMod.ACTOR_ENTITY,client.level); owner.setForm(nativeForm); owner.setItemSlot(EquipmentSlot.MAINHAND,gun);
        var source=new MCEntity(owner); source.setHotbarStack(0,gun);
        var nativeRenderer=(TaczFormRenderer)FormUtilsClient.getRenderer(nativeForm); nativeRenderer.tick(source); var player=nativeRenderer.visualPlayer();
        var replay=new Replay("catalog"); replay.form.set(new TaczForm()); var restored=new StubEntity(client.level); restored.setForm(replay.form.get());
        var replayRenderer=(TaczFormRenderer)FormUtilsClient.getRenderer(restored.getForm());
        var clips=animations.get(display.getPlayerAnimator3rd());
        if(clips==null||clips.isEmpty()) { rows.add(Map.of("gun",gunEntry.getKey().toString(),"status","not_applicable","reason","no native player-animator clips in loaded gun display")); owner.discard(); return false; }
        var names=new ArrayList<>(clips.keySet()); names.sort(String::compareTo);
        for(String motion:MOTIONS) for(String name:names) {
            PhysicalCrawl.release(owner); owner.setSwimming(false); owner.setPose(Pose.STANDING); source.setSneaking(false); source.setSprinting(false); source.setRiding(false); source.setFlying(false); source.setOnGround(true);
            source.setPosition(0,-60,0); source.setPrevX(0); source.setPrevY(-60); source.setPrevZ(0);
            boolean moving=!motion.equals("stand")&&!motion.equals("ride");
            if(motion.equals("sprint")) source.setSprinting(true);
            if(motion.equals("crouch_walk")) source.setSneaking(true);
            if(motion.equals("crawl_move")) PhysicalCrawl.apply(owner,true);
            if(motion.equals("airborne")) { source.setOnGround(false); source.setFlying(true); }
            if(motion.equals("ride")) source.setRiding(true);
            if(motion.equals("swim")) { owner.setSwimming(true); owner.setPose(Pose.SWIMMING); }
            float speed=moving?motion.equals("sprint")?0.3f:0.15f:0;
            source.setVelocity(speed,motion.equals("airborne")?0.12f:0,0.04f);
            var limb=(LimbAnimatorAccessor)source.getLimbAnimator(); limb.setPrevSpeed(speed); limb.setSpeed(speed); limb.setPos(2);
            player.sync(source,null,0); AnimationManager.stopAllAnimation(player,0); PlayerAnimatorCompat.playAnimation(player,display,speed);
            var layer=(ModifierLayer<IAnimation>)PlayerAnimationAccess.getPlayerAssociatedData(player).get(PlayerAnimatorCompat.ONCE_UPPER_ANIMATION);
            ReviewClient.require(layer!=null,"Native animation layer missing for "+gunEntry.getKey());
            float motionDelta=0,maxError=0; int pixels=0;
            KeyframeAnimation clip=clips.get(name);
            for(int frame:new int[]{0,12,24}) {
                if(moving) { var before=owner.position(); owner.move(MoverType.SELF,new Vec3(speed,motion.equals("airborne")?0.12:0,0.04)); motionDelta+=(float)owner.position().distanceTo(before); }
                limb.setPos(2+frame*speed); player.sync(source,null,0);
                layer.setAnimation(new KeyframeAnimationPlayer(clip,frame));
                var pose=ThirdPersonPose.sample(player,0,false);
                var snapshot=new FrameSnapshot(gun.save(new CompoundTag()).toString(),false,Map.of(),pose,PoseMath.identity(),true,motion.equals("crawl_move"),0,0,-1,1000,0,List.of(),new float[]{0,0,0,1},List.of());
                var copy=FrameFactory.INSTANCE.fromData(FrameFactory.INSTANCE.toData(snapshot));
                ReviewClient.require(FrameFactory.INSTANCE.compare(snapshot,copy),"Catalog pose codec mismatch");
                ((TaczReplayChannels)replay.keyframes).bbstacz$state().insert(0,copy); replay.keyframes.apply(0,restored);
                restored.setPosition(owner.getX(),owner.getY(),owner.getZ()); restored.setSprinting(source.isSprinting()); restored.setSneaking(source.isSneaking()); restored.setRiding(source.isRiding()); restored.setFlying(source.isFlying()); restored.setOnGround(source.isOnGround());
                final float[] difference={0}; final int[] draws={0};
                ReviewProbe.playerDraw=draw->{ if(draw.scope().entity==restored) {
                    difference[0]=Math.max(difference[0],ReviewProbe.armError(draw,copy.humanoid())); draws[0]++;
                }};
                pixels=NativePreviewSmoke.renderCompact(client,replayRenderer,restored,null,motion.equals("crawl_move"),35,64);
                maxError=Math.max(maxError,difference[0]);
                ReviewClient.require(draws[0]>0&&pixels>20&&difference[0]<0.065,"Catalog native/baked GPU mismatch "+gunEntry.getKey()+" "+motion+" "+name+" frame="+frame+" error="+difference[0]);
                ReviewProbe.playerDraw=null; ReplayBindings.release(restored);
            }
            ReviewClient.require(!moving||motionDelta>0.1,"Catalog moving case was stationary");
            rows.add(Map.of("gun",gunEntry.getKey().toString(),"motion",motion,"animation",name,"status","pass","sampled_frames",List.of(0,12,24),"native_actor_displacement",motionDelta,"max_arm_rotation_error",maxError,"pixels",pixels,"level","controlled native PA / moving actor / codec / GPU"));
        }
        PhysicalCrawl.release(owner); owner.discard();
        System.out.println("[REVIEW catalog gun] "+gunEntry.getKey()+" clips="+names.size()+" cases="+(MOTIONS.length*names.size())+" rows="+rows.size()); return false;
    }
}
