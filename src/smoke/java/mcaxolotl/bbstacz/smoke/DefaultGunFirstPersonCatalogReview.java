package mcaxolotl.bbstacz.smoke;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.AnimationController;
import com.tacz.guns.api.client.animation.ObjectAnimation;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.client.particle.ThirdPersonMuzzleParticleManager;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import mcaxolotl.bbstacz.client.NativePose;
import mcaxolotl.bbstacz.recording.FrameFactory;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.PoseMath;
import mchorse.bbs_mod.forms.FormUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;

/** All loaded FP animation prototypes, normalized native-runner sampling, codec/restoration and actual gun draws.
 * This catalog is controlled native evaluation, not a claim that every gameplay transition is legal. */
public final class DefaultGunFirstPersonCatalogReview implements ReviewClient.Scenario {
    private final ArrayList<Map<String,Object>> rows=new ArrayList<>();
    private List<Map.Entry<ResourceLocation,com.tacz.guns.client.resource.index.ClientGunIndex>> guns;
    private int cursor;
    @Override public int timeoutTicks() { return 14000; }
    @SuppressWarnings("unchecked")
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(guns==null) { guns=new ArrayList<>(TimelessAPI.getAllClientGunIndex()); guns.sort(Comparator.comparing(e->e.getKey().toString())); }
        if(cursor>=guns.size()) {
            Path report=Path.of("bbs-tacz-first-person-catalog.json"); Files.writeString(report,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));
            System.out.println("[REVIEW FP catalog] guns="+guns.size()+" rows="+rows.size()+" report="+report.toAbsolutePath()); return true;
        }
        var entry=guns.get(cursor++); var display=entry.getValue().getDefaultDisplay(); var model=display.getGunModel();
        ReviewClient.require(model!=null,"Loaded stock gun has no native FP bedrock model: "+entry.getKey());
        var gun=GunItemBuilder.create().setId(entry.getKey()).setAmmoCount(20).setAmmoInBarrel(true).build(); client.player.setItemSlot(EquipmentSlot.MAINHAND,gun);
        var renderer=GunItemRendererWrapper.INSTANCE.get(); renderer.tryInit(gun,client.player,0);
        var state=display.getAnimationStateMachine(); ReviewClient.require(state!=null&&state.isInitialized(),"FP state machine did not initialize: "+entry.getKey());
        var controller=state.getAnimationController();
        var field=AnimationController.class.getDeclaredField("prototypes"); field.setAccessible(true);
        var prototypes=(Map<String,ObjectAnimation>)field.get(controller);
        var names=new ArrayList<>(prototypes.keySet()); names.sort(String::compareTo);
        var saved=NativePose.capture(model); var oldTracks=controller.getUpdatingTrackArray(); controller.setUpdatingTrackArray(List.of(0));
        for(String name:names) {
            ObjectAnimation prototype=prototypes.get(name); float duration=prototype.getMaxEndTimeS();
            ReviewClient.require(Float.isFinite(duration)&&duration>=0,"Invalid native animation duration "+entry.getKey()+" "+name);
            int minPixels=Integer.MAX_VALUE; boolean changed=false; Map<String,mcaxolotl.bbstacz.recording.PartPose> previous=null;
            for(float phase:new float[]{0,0.25f,0.5f,0.9f}) {
                NativePose.apply(model,saved); controller.runAnimation(0,name,ObjectAnimation.PlayType.PLAY_ONCE_HOLD,0);
                var runner=controller.getAnimation(0); ReviewClient.require(runner!=null,"Native FP prototype not runnable: "+name);
                runner.pause(); runner.setProgressNs((long)(duration*phase*1e9)); controller.update();
                var pose=NativePose.capture(model); if(previous!=null&&!pose.equals(previous)) changed=true; previous=pose;
                var snapshot=new FrameSnapshot(gun.save(new CompoundTag()).toString(),true,pose,Map.of(),PoseMath.identity(),model.getRenderHand(),false,0,0,-1,1000,0,List.of(),new float[]{0,0,0,1},List.of());
                var copy=FrameFactory.INSTANCE.fromData(FrameFactory.INSTANCE.toData(snapshot)); ReviewClient.require(FrameFactory.INSTANCE.compare(snapshot,copy),"FP native animation codec failed: "+name);
                int referencePixels=draw(client,model,gun,display.getModelTexture());
                NativePose.apply(model,saved); NativePose.apply(model,copy.gunBones()); ReviewClient.require(NativePose.capture(model).equals(pose),"FP restored bone values differ: "+name);
                int restoredPixels=draw(client,model,gun,display.getModelTexture());
                ReviewClient.require(Math.abs(referencePixels-restoredPixels)<12,"Native/restored gun visibility differs "+entry.getKey()+" "+name+" reference="+referencePixels+" restored="+restoredPixels);
                minPixels=Math.min(minPixels,restoredPixels);
                controller.consumePendingParticles();
            }
            rows.add(Map.of("gun",entry.getKey().toString(),"animation",name,"duration_seconds",duration,"normalized_samples",List.of(0,0.25,0.5,0.9),"bone_values_changed",changed,"min_visible_pixels",minPixels,"status","pass","level","controlled native FP runner / codec / restored bones / GPU"));
        }
        controller.removeAnimation(0); controller.setUpdatingTrackArray(oldTracks); NativePose.apply(model,saved);
        System.out.println("[REVIEW FP catalog gun] "+entry.getKey()+" prototypes="+names.size()+" rows="+rows.size()); return false;
    }
    private int draw(Minecraft client,com.tacz.guns.client.model.BedrockGunModel model,net.minecraft.world.item.ItemStack gun,ResourceLocation texture) {
        ((cn.sh1rocu.tacz.api.mixin.RenderTargetStencil)(Object)client.getMainRenderTarget()).tacz$enableStencil();
        var target=new TextureTarget(64,64,true,Minecraft.ON_OSX); ((cn.sh1rocu.tacz.api.mixin.RenderTargetStencil)(Object)target).tacz$enableStencil(); var projection=new Matrix4f(RenderSystem.getProjectionMatrix()); var sorting=RenderSystem.getVertexSorting(); var view=RenderSystem.getModelViewStack();
        var oldEntity=ThirdPersonMuzzleParticleManager.currentEntity; boolean muzzle=MuzzleFlashRender.isSelf,shell=ShellRender.isSelf;
        view.pushPose(); view.setIdentity(); RenderSystem.applyModelViewMatrix();
        try {
            target.setClearColor(0.04f,0.05f,0.06f,1); target.clear(Minecraft.ON_OSX); target.bindWrite(true); RenderSystem.clearStencil(0); RenderSystem.clear(org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT,Minecraft.ON_OSX);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-2.5f,2.5f,-2.5f,2.5f,-100,100),VertexSorting.ORTHOGRAPHIC_Z); Lighting.setupForEntityInInventory(); client.gameRenderer.lightTexture().turnOnLightLayer();
            ThirdPersonMuzzleParticleManager.currentEntity=client.player; MuzzleFlashRender.isSelf=false; ShellRender.isSelf=false;
            var pose=new PoseStack(); pose.translate(0,-0.5,-8); pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(35));
            var provider=FormUtilsClient.getProvider(); model.render(pose,gun,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,RenderType.entityCutout(texture),15728880,OverlayTexture.NO_OVERLAY,provider); provider.endBatch();
            try(var image=Screenshot.takeScreenshot(target)) { ObsReviewSession.evidence(image); int pixels=0,background=image.getPixelRGBA(0,0); for(int y=0;y<64;y++) for(int x=0;x<64;x++) if(image.getPixelRGBA(x,y)!=background) pixels++; return pixels; }
        } finally {
            ThirdPersonMuzzleParticleManager.currentEntity=oldEntity; MuzzleFlashRender.isSelf=muzzle; ShellRender.isSelf=shell; target.destroyBuffers(); client.getMainRenderTarget().bindWrite(true); client.gameRenderer.lightTexture().turnOffLightLayer();
            view.popPose(); RenderSystem.applyModelViewMatrix(); RenderSystem.setProjectionMatrix(projection,sorting);
        }
    }
}
