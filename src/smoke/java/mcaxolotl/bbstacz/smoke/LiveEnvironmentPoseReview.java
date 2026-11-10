package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mcaxolotl.bbstacz.client.ThirdPersonPose;
import mcaxolotl.bbstacz.client.forms.TaczFormRenderer;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.FrameFactory;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.PoseMath;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.Vec3;

/** Real keyboard/vanilla physics in an isolated survival world, native gun events and restored GPU body poses.
 * Pools, ladders and vehicles are fixtures; movement/swimming/climbing/gliding are not forced pose flags. */
public final class LiveEnvironmentPoseReview implements ReviewClient.Scenario {
    private static final String[] CASES={"backward_walk","strafe_turn","ladder_climb","swim","boat_ride","elytra_glide","creative_flight"};
    private final List<Map<String,Object>> rows=new ArrayList<>();
    private int index=-1,start,shotCount,clientShots,shotStart,reloads,melees,reloadStart,meleeStart;
    private boolean stateSeen,shot;
    private double x,y,z,distance;
    private TaczForm form;
    private ItemStack gun;
    private boolean sampled;
    public LiveEnvironmentPoseReview() {
        com.tacz.guns.api.event.common.GunShootEvent.CALLBACK.register(e->{if(e.getLogicalSide().isClient()) clientShots++;});
        com.tacz.guns.api.event.common.GunReloadEvent.CALLBACK.register(e->{if(e.getLogicalSide().isClient()) reloads++;});
        com.tacz.guns.api.event.common.GunMeleeEvent.CALLBACK.register(e->{if(e.getLogicalSide().isClient()) melees++;});
    }
    @Override public int timeoutTicks() { return 1700; }
    private void release(Minecraft client) {
        client.options.keyUp.setDown(false); client.options.keyDown.setDown(false); client.options.keyRight.setDown(false);
        client.options.keyLeft.setDown(false); client.options.keyJump.setDown(false); client.options.keySprint.setDown(false);
        com.tacz.guns.client.input.ShootKey.SHOOT_KEY.setDown(false);
        client.options.keyShift.setDown(false); client.player.setSprinting(false);
        IClientPlayerGunOperator.fromLocalPlayer(client.player).aim(false);
        IClientPlayerGunOperator.fromLocalPlayer(client.player).crawl(false);
    }
    private void setup(Minecraft client,int tick) {
        release(client); start=tick; stateSeen=shot=sampled=false; distance=0; reloadStart=reloads; meleeStart=melees; shotStart=clientShots;
        var uuid=client.player.getUUID(); int motion=index;
        client.getSingleplayerServer().submit(()-> {
            var p=client.getSingleplayerServer().getPlayerList().getPlayer(uuid); p.stopRiding(); p.stopFallFlying();
            p.setGameMode(motion==6?GameType.CREATIVE:GameType.SURVIVAL); p.getAbilities().flying=motion==6; p.onUpdateAbilities();
            p.setItemSlot(EquipmentSlot.CHEST,motion==5?new ItemStack(Items.ELYTRA):ItemStack.EMPTY);
            double px=0.5,py=-60,pz=0.5;
            if(motion==2) { px=0.5; pz=18.5; }
            if(motion==3) { px=4.5; py=-58; pz=40.5; }
            if(motion==4) { px=14.5; py=-56.5; pz=42.5; }
            if(motion==5||motion==6) { px=0.5; py=10; pz=100.5; }
            p.teleportTo(px,py,pz); p.setYRot(0); p.setXRot(motion==5?-15:0); p.setDeltaMovement(Vec3.ZERO); p.setOnGround(false);
            if(motion==4) { var boat=EntityType.BOAT.create(p.serverLevel()); boat.setPos(px,py,pz); p.serverLevel().addFreshEntity(boat); p.startRiding(boat,true); }
        }).join();
        client.player.setYRot(0); client.player.setXRot(index==5?-15:0);
        x=client.player.getX(); y=client.player.getY(); z=client.player.getZ();
    }
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick==0) {
            client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            form=new TaczForm(); Morph.getMorph(client.player).setForm(form);
            gun=GunItemBuilder.create().setId(new ResourceLocation("tacz","ak47")).setAmmoCount(30).setAmmoInBarrel(true).setFireMode(FireMode.SEMI).build();
            client.player.setItemSlot(EquipmentSlot.MAINHAND,gun); AttachmentPropertyManager.postChangeEvent(client.player,gun);
            var uuid=client.player.getUUID(); var ammoId=TimelessAPI.getCommonGunIndex(new ResourceLocation("tacz","ak47")).orElseThrow().getGunData().getAmmoId();
            client.getSingleplayerServer().submit(()-> {
                var p=client.getSingleplayerServer().getPlayerList().getPlayer(uuid); p.setItemSlot(EquipmentSlot.MAINHAND,gun.copy()); AttachmentPropertyManager.postChangeEvent(p,gun);
                for(int slot=9;slot<18;slot++) p.getInventory().setItem(slot,AmmoItemBuilder.create().setId(ammoId).setCount(64).build());
                var level=p.serverLevel();
                for(int bx=0;bx<24;bx++) for(int bz=30;bz<95;bz++) for(int by=-60;by<=-56;by++) level.setBlock(new BlockPos(bx,by,bz),Blocks.WATER.defaultBlockState(),2);
                for(int by=-60;by<-25;by++) { level.setBlock(new BlockPos(0,by,20),Blocks.STONE.defaultBlockState(),2); level.setBlock(new BlockPos(0,by,19),Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.NORTH),2); }
            }).join();
        }
        if(index<0&&tick>=35&&ReviewProbe.liveGunDraws>8) { index=0; setup(client,tick); }
        if(index<0) return false;
        int step=tick-start; var op=IClientPlayerGunOperator.fromLocalPlayer(client.player);
        if(step==15) { x=client.player.getX(); y=client.player.getY(); z=client.player.getZ(); }
        if(step>=15&&step<155) {
            if(index==0) client.options.keyDown.setDown(true);
            else if(index==1) { client.options.keyRight.setDown(true); client.player.setYRot(step*2); client.player.setXRot((float)Math.sin(step/10d)*20); }
            else client.options.keyUp.setDown(true);
            if(index==3) client.options.keySprint.setDown(true);
            if(index==5&&step<45) client.options.keyJump.setDown(step%8<4);
            if(index==5&&step>=45) client.player.setYRot(step);
        }
        boolean present=switch(index) {
            case 2 -> client.player.onClimbable(); case 3 -> client.player.isSwimming(); case 4 -> client.player.isPassenger();
            case 5 -> client.player.isFallFlying(); case 6 -> client.player.getAbilities().flying&&!client.player.onGround(); default -> true;
        };
        stateSeen|=present;
        if(step>=15) distance=Math.max(distance,client.player.position().distanceTo(new Vec3(x,y,z)));
        com.tacz.guns.client.input.ShootKey.SHOOT_KEY.setDown(step>=30&&step<55&&!shot);
        if(!shot&&clientShots>shotStart) { shot=true; shotCount++; }
        if(step>=60&&step<100&&reloads==reloadStart&&!op.getDataHolder().clientStateLock) op.reload();
        if(step>=110&&step<150&&melees==meleeStart&&!op.getDataHolder().clientStateLock) op.melee();
        if(step>=40&&present&&!sampled&&(index!=5||client.player.getFallFlyingTicks()>8)) {
            var renderer=(TaczFormRenderer)FormUtilsClient.getRenderer(form); var source=new MCEntity(client.player);
            renderer.visualPlayer().sync(source,null);
            ReviewClient.require(renderer.visualPlayer().isFallFlying()==client.player.isFallFlying(),"Visual player lost live fall-flying flag");
            ReviewClient.require(renderer.visualPlayer().getFallFlyingTicks()==client.player.getFallFlyingTicks(),"Visual player lost native fall-flying rotation phase: "+renderer.visualPlayer().getFallFlyingTicks()+"/"+client.player.getFallFlyingTicks());
            if(index==2||index==3||index==5) ReviewClient.require(!PlayerAnimatorCompat.canPlayAnimation(client.player),"Native PA did not apply its swimming/climbing/gliding guard");
            var pose=ThirdPersonPose.sample(client.player,0);
            var frame=new FrameSnapshot(client.player.getMainHandItem().save(new CompoundTag()).toString(),false,Map.of(),pose,PoseMath.identity(),true,false,0,0,-1,1000,0,List.of(),new float[]{0,0,0,1},List.of());
            var restored=FrameFactory.INSTANCE.fromData(FrameFactory.INSTANCE.toData(frame)); ReviewClient.require(FrameFactory.INSTANCE.compare(frame,restored),"Environment body pose codec changed");
            var actor=new StubEntity(client.level); actor.setForm(new TaczForm()); actor.setPosition(client.player.getX(),client.player.getY(),client.player.getZ());
            actor.setFallFlying(client.player.isFallFlying()); actor.setSwimming(client.player.isSwimming()); actor.setFlying(client.player.getAbilities().flying);
            var restoredRenderer=(TaczFormRenderer)FormUtilsClient.getRenderer(actor.getForm()); restoredRenderer.visualPlayer().sync(actor,restored);
            ReviewClient.require(restoredRenderer.visualPlayer().getFallFlyingTicks()==client.player.getFallFlyingTicks(),"Serialized body pose lost native glide rotation phase");
            var replay=new mchorse.bbs_mod.film.replays.Replay("env_pose"); ((mcaxolotl.bbstacz.recording.TaczReplayChannels)replay.keyframes).bbstacz$state().insert(0,restored);
            mcaxolotl.bbstacz.recording.ReplayBindings.bind(actor,((mcaxolotl.bbstacz.recording.TaczReplayChannels)replay.keyframes).bbstacz$state(),0);
            int pixels=NativePreviewSmoke.renderCompact(client,restoredRenderer,actor,null,false,35,64);
            ReviewClient.require(pixels>0,"Restored environment body rendered no pixels: "+CASES[index]); mcaxolotl.bbstacz.recording.ReplayBindings.release(actor); sampled=true;
        }
        if(index==5&&client.player.isFallFlying()&&step>=60&&step%20==0) {
            var renderer=(TaczFormRenderer)FormUtilsClient.getRenderer(form); renderer.visualPlayer().sync(new MCEntity(client.player),null);
            var access=(mchorse.bbs_mod.mixin.client.LivingEntityRendererInvoker)client.getEntityRenderDispatcher().getRenderer(client.player);
            for(float fraction:new float[]{0,0.5f,1}) {
                var ref=new com.mojang.blaze3d.vertex.PoseStack(); var visual=new com.mojang.blaze3d.vertex.PoseStack();
                float yaw=net.minecraft.util.Mth.rotLerp(fraction,client.player.yBodyRotO,client.player.yBodyRot);
                access.bbs$setupTransforms(client.player,ref,client.player.tickCount+fraction,yaw,fraction);
                access.bbs$setupTransforms(renderer.visualPlayer(),visual,client.player.tickCount+fraction,0,fraction);
                var equivalent=new org.joml.Matrix4f().rotateY((float)-Math.toRadians(yaw)).mul(visual.last().pose());
                float[] expected=ref.last().pose().get(new float[16]),actual=equivalent.get(new float[16]);
                for(int i=0;i<16;i++) ReviewClient.require(Math.abs(expected[i]-actual[i])<0.02,"Turning glide root differs from the native player at fraction "+fraction+" matrix entry "+i+": "+expected[i]+"/"+actual[i]);
            }
        }
        if(step>=180) {
            release(client); ReviewClient.require(stateSeen&&distance>1&&sampled,"Native movement was not actually exercised: "+CASES[index]+" state="+stateSeen+" distance="+distance+" sampled="+sampled);
            if(!shot) System.out.println("[REVIEW input diagnostic] camera="+client.options.getCameraType()+" screen="+client.screen+" direct_result="+op.shoot()+" draws="+ReviewProbe.liveGunDraws);
            ReviewClient.require(shot&&reloads>reloadStart&&melees>meleeStart,"Native environment gun action not accepted: "+CASES[index]+" shot="+shot+" reload="+(reloads-reloadStart)+" melee="+(melees-meleeStart));
            rows.add(Map.of("motion",CASES[index],"distance",distance,"native_shots",1,"reload_events",reloads-reloadStart,"melee_events",melees-meleeStart,"status","PASS")); System.out.println("[REVIEW live environment] "+rows.get(rows.size()-1));
            if(++index==CASES.length) { java.nio.file.Files.writeString(java.nio.file.Path.of("bbs-tacz-live-environment.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows)); ReviewClient.require(shotCount==CASES.length,"Missing environment shots"); return true; }
            setup(client,tick);
        }
        return false;
    }
}
