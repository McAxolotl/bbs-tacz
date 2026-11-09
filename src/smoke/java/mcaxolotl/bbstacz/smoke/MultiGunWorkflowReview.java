package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import java.nio.file.Path;
import java.util.HashSet;
import mcaxolotl.bbstacz.actions.TaczProjectileActionClip;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import mcaxolotl.bbstacz.recording.NativeSoundTimeline;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Five real gun classes, refit/inspect interruptions, ADS, walking/sprinting/crawl through actual Alt/Ctrl/F6 keys. */
public class MultiGunWorkflowReview extends HotkeyWorkflowReview {
    protected static final String[] GUNS={"ak47","m4a1","glock_17","aa12","rpg7"};
    private final HashSet<String> liveShots=new HashSet<>();
    private int refitViews;
    private long playbackBefore;
    protected int successfulWeapons() { return liveShots.size(); }
    @Override public int timeoutTicks() { return 2800; }
    @Override protected int recordTicks() { return GUNS.length*150+80; }
    @Override protected boolean ctrlFinished(Minecraft client,int elapsed) {
        var controller=BBSModClient.getFilms().getController(film.getId()); return controller==null || controller.getTick()>=film.calculateDuration()-2;
    }
    @Override protected void act(Minecraft client,int time) {
        int index=time/150,step=time%150;
        if(index>=GUNS.length) {
            var idle=IClientPlayerGunOperator.fromLocalPlayer(client.player);
            if(time<recordTicks()-2) { com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(true); idle.aim(true); }
            else { com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(false); idle.aim(false); idle.crawl(false); }
            if(time==GUNS.length*150+40) System.out.println("[REVIEW native ADS] aim="+idle.isAim()+" progress="+idle.getClientAimingProgress(0)+" serverSyn="+com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(client.player).getSynAimingProgress());
            return;
        }
        if(step>=12) step-=18;
        // Switching also includes the old gun's put-away time. Start the combination matrix
        // only after the slowest native launcher draw, rather than bypassing its state lock.
        if(time%150>=12 && time%150<30) return;
        var operator=IClientPlayerGunOperator.fromLocalPlayer(client.player);
        if(step==0) {
            client.options.keyUp.setDown(false); client.options.keySprint.setDown(false); com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(false); client.player.setSprinting(false); operator.crawl(false);
            var id=new ResourceLocation("tacz",GUNS[index]); var data=TimelessAPI.getCommonGunIndex(id).orElseThrow().getGunData();
            var last=client.player.getMainHandItem().copy();
            var gun=GunItemBuilder.create().setId(id).setAmmoCount(data.getAmmoAmount()).setAmmoInBarrel(true).setFireMode(data.getFireModeSet().get(0)).build();
            client.player.setItemSlot(EquipmentSlot.MAINHAND,gun); AttachmentPropertyManager.postChangeEvent(client.player,gun);
            var uuid=client.player.getUUID(); client.getSingleplayerServer().execute(()-> { var p=client.getSingleplayerServer().getPlayerList().getPlayer(uuid); p.setItemSlot(EquipmentSlot.MAINHAND,gun.copy()); AttachmentPropertyManager.postChangeEvent(p,gun); });
            operator.draw(last); System.out.println("[REVIEW combinations] weapon="+id);
        }
        var machine=GunItemRendererWrapper.INSTANCE.get().getStateMachine(client.player.getMainHandItem());
        if(step==4) { client.options.keyUp.setDown(true); com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(true); }
        if(step==12) {
            client.options.keyUp.setDown(false); client.player.setXRot(-20);
            var result=operator.shoot(); System.out.println("[REVIEW combinations] actual shoot "+GUNS[index]+"="+result);
            ReviewClient.require(result==ShootResult.SUCCESS,"Real multi-gun shooting was rejected for "+GUNS[index]+": "+result); liveShots.add(GUNS[index]);
        }
        if(step==18) { com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(true); operator.aim(true); }
        if(step==20&&machine!=null) machine.trigger("inspect");
        if(step==24) { com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(false); operator.aim(false); }
        if(step==25||step==74||step==90) client.setScreen(new GunRefitScreen());
        if(step==32||step==50) { if(RefitTransform.changeRefitScreenView(AttachmentType.SCOPE)) refitViews++; }
        if(step==38||step==56||step==98) { if(RefitTransform.changeRefitScreenView(AttachmentType.MUZZLE)) refitViews++; }
        if(step==44) { if(RefitTransform.changeRefitScreenView(AttachmentType.GRIP)) refitViews++; }
        if(step==82) { if(RefitTransform.changeRefitScreenView(AttachmentType.STOCK)) refitViews++; }
        if(step==62||step==88||step==104) { if(client.screen!=null) client.screen.onClose(); }
        if(step==64) { operator.reload(); if(machine!=null) machine.trigger("reload"); }
        if(step==68) { operator.melee(); if(machine!=null) machine.trigger("bayonet_push"); }
        if(step==72&&machine!=null) machine.trigger("inspect");
        if(step==106&&machine!=null) machine.trigger("reload");
        if(step==108&&machine!=null) machine.trigger("shoot");
        if(step==110) { client.options.keyUp.setDown(true); client.options.keySprint.setDown(true); client.player.setSprinting(true); }
        if(step==114) { client.options.keySprint.setDown(false); client.player.setSprinting(false); operator.crawl(true); }
        // Keep crawling until the next gun switch so a complete native render can sample it.
        if(step==118) client.options.keyUp.setDown(false);
    }
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        ProjectileProbe.phase=phase==2?"record":phase==4||phase==5?"ctrl":phase==7?"f6":"ignore";
        if(phase==3&&playbackBefore==0) playbackBefore=FirstPersonPlayback.renderedFrames();
        return super.tick(client,tick);
    }
    @Override protected void verifyCtrl(Minecraft client) throws Exception {
        ReviewClient.require(liveShots.size()==GUNS.length,"Not every real gun fired");
        ReviewClient.require(ProjectileProbe.draws("record")>0&&ProjectileProbe.draws("ctrl")>0&&ProjectileProbe.duplicateDraws==0,"Multi-gun Alt/Ctrl tracers were missing or drawn twice per frame");
        var channels=(TaczReplayChannels)film.replays.getList().get(0).keyframes;
        var rendered=new HashSet<String>(); boolean ads=false,crawl=false,refit=false;
        for(var key:channels.bbstacz$state().getKeyframes()) {
            var frame=key.getValue(); if(!frame.firstPerson()||frame.gunStack().isEmpty()) continue;
            var stack=ItemStack.of(TagParser.parseTag(frame.gunStack())); var gun=IGun.getIGunOrNull(stack);
            if(gun!=null&&!frame.gunBones().isEmpty()) rendered.add(gun.getGunId(stack).getPath());
            ads|=frame.aiming()>0.1f; crawl|=frame.crawling(); refit|=frame.refit()>0.1f;
        }
        ReviewClient.require(rendered.containsAll(java.util.List.of(GUNS)),"Evaluated native FP poses are missing for guns: "+rendered);
        System.out.println("[REVIEW combinations coverage] ADS="+ads+" crawl="+crawl+" refit="+refit+" views="+refitViews);
        ReviewClient.require(ads&&crawl&&refit&&refitViews>=GUNS.length,"ADS/crawl/refit combinations were not evaluated");
        String commands=channels.bbstacz$commands().getKeyframes().stream().map(k->k.getValue()).collect(java.util.stream.Collectors.joining(" "));
        ReviewClient.require(commands.contains("refit_open")&&commands.contains("refit_close")&&commands.contains("refit_view"),"Repeated attachment-view/open/close commands did not survive recording");
        var triggers=new java.util.HashMap<String,Integer>();
        for(var key:channels.bbstacz$commands().getKeyframes()) for(var element:com.google.gson.JsonParser.parseString(key.getValue()).getAsJsonArray()) {
            var command=element.getAsJsonObject();
            if(command.get("op").getAsString().equals("trigger")) triggers.merge(command.getAsJsonArray("args").get(0).getAsString(),1,Integer::sum);
        }
        for(String trigger:new String[]{"inspect","reload","bayonet_push","shoot"})
            ReviewClient.require(triggers.getOrDefault(trigger,0)>=GUNS.length,"The five-gun interruption matrix did not record enough "+trigger+" commands: "+triggers);
        System.out.println("[REVIEW combinations triggers] "+triggers);
        var cues=NativeSoundTimeline.read(channels.bbstacz$sounds());
        ReviewClient.require(cues.size()>=GUNS.length&&film.camera.getClips(mchorse.bbs_mod.camera.clips.misc.AudioClip.class).isEmpty(),"Multi-gun sounds were missing or extracted to clips");
        ReviewClient.require(FirstPersonPlayback.renderedFrames()>playbackBefore+100,"Ctrl did not play the entire multi-gun first-person film");
        ReviewClient.require(film.replays.getList().get(0).actions.getClips(TaczProjectileActionClip.class).size()>GUNS.length,"Native shotgun pellets/launcher projectiles were not recorded");
        System.out.println("[REVIEW combinations] evaluated guns="+rendered+" successful refit views="+refitViews+" native sounds="+cues.size()+" CTRL FP draws="+(FirstPersonPlayback.renderedFrames()-playbackBefore));
    }
    @Override protected void verifyVideo(Minecraft client,Path video) throws Exception {
        ReviewClient.require(ProjectileProbe.draws("f6")>0&&ProjectileProbe.duplicateDraws==0,"F6 multi-gun tracers were missing or drawn twice per frame");
        System.out.println("[REVIEW combinations tracers] record/ctrl/f6="+ProjectileProbe.draws("record")+"/"+ProjectileProbe.draws("ctrl")+"/"+ProjectileProbe.draws("f6")+" duplicates="+ProjectileProbe.duplicateDraws);
        double world=(ReviewProbe.lastWorldTime-ReviewProbe.firstWorldTime)/20d,gun=(ReviewProbe.lastGunTime-ReviewProbe.firstGunTime)/1e9;
        ReviewClient.require(Math.abs(world-gun)<0.02,"F6 multi-gun/world clocks disagree");
        var process=new ProcessBuilder("/usr/bin/ffprobe","-v","error","-show_entries","stream=codec_name","-of","csv=p=0",video.toString()).start();
        String codecs=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        ReviewClient.require(process.waitFor()==0&&codecs.contains("h264")&&codecs.contains("aac"),"Multi-gun F6 export lacks video/native audio");
        System.out.println("[REVIEW combinations] F6 synchronized seconds="+world+" native gun seconds="+gun+" video="+video);
    }
}
