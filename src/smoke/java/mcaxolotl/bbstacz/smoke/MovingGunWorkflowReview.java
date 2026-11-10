package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import mcaxolotl.bbstacz.client.RecordedAudio;
import mcaxolotl.bbstacz.recording.NativeSoundTimeline;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import net.minecraft.client.Minecraft;

/** Real keyboard motion + accepted gun events across six movement states, Alt/Ctrl/F6 and body/GPU comparisons. */
public class MovingGunWorkflowReview extends HotkeyWorkflowReview {
    private static final String[] MODES={"stand","walk","sprint_to_aim","crouch_walk","crawl_move","jump"};
    private final ArrayList<Map<String,Object>> coverage=new ArrayList<>();
    private int reloads,melees,reloadStart,meleeStart,shots,refitViews,nativeShots,shotStart;
    private boolean shotCounted;
    private int activeMotion=-1;
    private final java.util.Map<String,Integer> shotResults=new java.util.HashMap<>();
    private boolean requestedMelee;
    private double x,z,y;
    private double displacement,height;
    private int bodyDraws;
    private long fpBefore;
    private boolean sprintSeen,crouchSeen,crawlSeen;
    private final java.util.Map<Integer,Integer> ctrlRendered=new java.util.HashMap<>();
    private final java.util.Map<Integer,Integer> exportRendered=new java.util.HashMap<>();
    private long lastFpDraws=-1;
    private int lastPreviewBin=-1;
    private boolean baselineStored;
    protected int completedMotions() { return coverage.size(); }
    protected int realShots() { return shots; }
    protected void writeMotionEvidence() throws Exception {
        Files.writeString(Path.of("bbs-tacz-live-motion-"+getClass().getSimpleName()+".json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(coverage));
    }
    private mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip cameraTrack;
    public MovingGunWorkflowReview() {
        ReviewProbe.shootResult=result->shotResults.merge(result.name(),1,Integer::sum);
        com.tacz.guns.api.event.common.GunShootEvent.CALLBACK.register(e->{if(e.getLogicalSide().isClient()) nativeShots++;});
        com.tacz.guns.api.event.common.GunReloadEvent.CALLBACK.register(e->{if(e.getLogicalSide().isClient()) reloads++;});
        com.tacz.guns.api.event.common.GunMeleeEvent.CALLBACK.register(e->{if(e.getLogicalSide().isClient()) melees++;});
        ReviewProbe.playerDraw=pose->{ if(!FirstPersonPlayback.isRendering()&&pose.scope().frame!=null&&!pose.scope().frame.humanoid().isEmpty()) {
            var expected=pose.scope().frame.humanoid().get("right_arm");
            ReviewClient.require(ReviewProbe.armError(pose,pose.scope().frame.humanoid())<0.07,"Moving replay GPU pose differs from its recorded body"); bodyDraws++;
            var binding=mcaxolotl.bbstacz.recording.ReplayBindings.get(pose.scope().entity);
            if(binding!=null&&binding.tick()<MODES.length*200) { int index=Math.max(0,(int)binding.tick()/200); if(phase==5) ctrlRendered.merge(index,1,Integer::sum); if(phase==7) {
                if(!firstPersonReplay()&&BBSModClient.getVideoRecorder().isRecording()&&binding.tick()>2) ReviewProbe.requireFramed(Minecraft.getInstance(),pose.scope().entity);
                exportRendered.merge(index,1,Integer::sum);
            } }
        }};
    }
    @Override public int timeoutTicks() { return 6200; }
    @Override protected int recordTicks() { return MODES.length*200+60; }
    @Override protected boolean ctrlFinished(Minecraft client,int elapsed) {
        var controller=BBSModClient.getFilms().getController(film.getId()); return controller==null||controller.getTick()>=film.calculateDuration()-2;
    }
    @Override protected void act(Minecraft client,int time) {
        if(time>=MODES.length*200) return;
        int phase=time/200,step=time%200; var operator=IClientPlayerGunOperator.fromLocalPlayer(client.player);
        if(activeMotion!=phase) {
            activeMotion=phase; shotResults.clear();
            client.options.keyUp.setDown(false); client.options.keySprint.setDown(false); client.options.keyShift.setDown(false); client.options.keyJump.setDown(false);
            com.tacz.guns.client.input.ShootKey.SHOOT_KEY.setDown(false);
            com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(false); operator.aim(false); operator.crawl(false); client.player.setSprinting(false);
            sprintSeen=crouchSeen=crawlSeen=false; x=client.player.getX(); z=client.player.getZ(); y=client.player.getY(); displacement=height=0; reloadStart=reloads; meleeStart=melees; requestedMelee=false; shotStart=nativeShots; shotCounted=false;
            if(phase!=0) client.options.keyUp.setDown(true);
            if(phase==2) client.options.keySprint.setDown(true);
            if(phase==3) client.options.keyShift.setDown(true);
            if(phase==4) operator.crawl(true);
            if(phase==5) client.options.keyJump.setDown(true);
        }
        sprintSeen|=client.player.isSprinting(); crouchSeen|=client.player.isCrouching(); crawlSeen|=mcaxolotl.bbstacz.recording.PhysicalCrawl.isCrawling(client.player);
        if(cameraTrack!=null) { cameraTrack.x.insert(time,client.player.getX()+4); cameraTrack.y.insert(time,client.player.getY()+2.5); cameraTrack.z.insert(time,client.player.getZ()-4); cameraTrack.yaw.insert(time,225d); cameraTrack.pitch.insert(time,15d); cameraTrack.fov.insert(time,70d); if(!film.camera.get().contains(cameraTrack)) film.camera.addClip(cameraTrack); }
        displacement=Math.max(displacement,Math.hypot(client.player.getX()-x,client.player.getZ()-z)); height=Math.max(height,client.player.getY()-y);
        if(step==16) { client.options.keySprint.setDown(false); client.player.setSprinting(false); com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(true); operator.aim(true); }
        // Use TACZ's trigger frontend: it owns sprint-stop and waits for the native/network readiness.
        // A one-off direct shoot on a hard-coded tick can legitimately return IS_SPRINTING on slow frames.
        com.tacz.guns.client.input.ShootKey.SHOOT_KEY.setDown(step>=25&&step<45&&!shotCounted);
        if(!shotCounted&&nativeShots>shotStart) { shots++; shotCounted=true; }
        if(step==30) { com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(false); operator.aim(false); }
        if(step>=40&&step<85&&reloads==reloadStart&&!operator.getDataHolder().clientStateLock) operator.reload();
        if(step>=90&&step<130&&!requestedMelee&&!operator.getDataHolder().clientStateLock) { operator.melee(); requestedMelee=melees>meleeStart; }
        var machine=GunItemRendererWrapper.INSTANCE.get().getStateMachine(client.player.getMainHandItem());
        if(step==130&&machine!=null) operator.inspect();
        if(step==138||step==170) client.setScreen(new GunRefitScreen());
        if(step==146&&RefitTransform.changeRefitScreenView(AttachmentType.SCOPE)) refitViews++;
        if(step==150&&RefitTransform.changeRefitScreenView(AttachmentType.MUZZLE)) refitViews++;
        if(step==174&&RefitTransform.changeRefitScreenView(AttachmentType.STOCK)) refitViews++;
        if(step==158||step==180) if(client.screen!=null) client.screen.onClose();
        if(step==46) System.out.println("[REVIEW moving trigger] motion="+MODES[phase]+" results="+shotResults+" screen="+client.screen+" spectator="+client.player.isSpectator()+" lock="+operator.getDataHolder().clientStateLock+" gun="+client.player.getMainHandItem()+" input="+com.tacz.guns.client.input.ShootKey.SHOOT_KEY.isDown());
        if(step==190) {
            client.options.keyUp.setDown(false); client.options.keySprint.setDown(false); client.options.keyShift.setDown(false); client.options.keyJump.setDown(false); operator.crawl(false);
            ReviewClient.require(phase==0||displacement>1,"Motion phase stayed stationary: "+MODES[phase]);
            ReviewClient.require(phase!=2||sprintSeen,"Sprint phase never actually sprinted");
            ReviewClient.require(phase!=3||crouchSeen,"Crouch phase never actually crouched");
            ReviewClient.require(phase!=4||crawlSeen,"Crawl phase never actually crawled");
            ReviewClient.require(phase!=5||height>0.4,"Jump phase did not actually leave ground");
            ReviewClient.require(shotCounted,"Native trigger did not actually fire in "+MODES[phase]+" results="+shotResults);
            ReviewClient.require(reloads>reloadStart&&melees>meleeStart,"Native moving reload/melee not accepted in "+MODES[phase]+" reload="+(reloads-reloadStart)+" melee="+(melees-meleeStart)+" lock="+operator.getDataHolder().clientStateLock);
            coverage.add(Map.of("motion",MODES[phase],"distance",displacement,"max_height",height,"reload_events",reloads-reloadStart,"melee_events",melees-meleeStart,"status","pass"));
            System.out.println("[REVIEW live movement] "+coverage.get(coverage.size()-1));
        }
    }
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick==0&&!firstPersonReplay()) { cameraTrack=new mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip(); cameraTrack.duration.set(recordTicks()+2); }
        if(phase==5&&!firstPersonReplay()) {
            var controller=BBSModClient.getFilms().getController(film.getId());
            // Sample five actual GPU poses per motion instead of allocating/readbacking a disposable
            // framebuffer on every playback tick (which can saturate driver synchronization).
            if(controller!=null&&controller.getTick()<MODES.length*200&&controller.getTick()/40!=lastPreviewBin) {
                lastPreviewBin=controller.getTick()/40;
                var actor=controller.getEntities().get(controller.film.replays.getList().get(0).getId());
                if(actor!=null&&actor.getForm()!=null) NativePreviewSmoke.renderCompact(client,(mcaxolotl.bbstacz.client.forms.TaczFormRenderer)mchorse.bbs_mod.forms.FormUtilsClient.getRenderer(actor.getForm()),actor,null,mcaxolotl.bbstacz.recording.ReplayBindings.sample(actor,0).crawling(),35,64);
            }
        }
        if(phase==3&&!baselineStored) { baselineStored=true; fpBefore=FirstPersonPlayback.renderedFrames(); System.out.println("[REVIEW motion FP data] before="+fpBefore+" camera="+client.options.getCameraType()+" fp keys="+((TaczReplayChannels)film.replays.getList().get(0).keyframes).bbstacz$state().getKeyframes().stream().filter(key->key.getValue().firstPerson()).count()); }
        if(phase==5&&tick-phaseTick==30) System.out.println("[REVIEW motion Ctrl FP] before="+fpBefore+" now="+FirstPersonPlayback.renderedFrames()+" camera="+client.options.getCameraType()+" target tick="+(FirstPersonPlayback.target(0)==null?-1:FirstPersonPlayback.target(0).tick()));
        long draws=FirstPersonPlayback.renderedFrames();
        if(firstPersonReplay()&&draws!=lastFpDraws) {
            var target=FirstPersonPlayback.target(0);
            if(target!=null&&target.tick()<MODES.length*200) { int index=Math.max(0,(int)target.tick()/200); if(phase==5) ctrlRendered.merge(index,1,Integer::sum); if(phase==7) exportRendered.merge(index,1,Integer::sum); }
            lastFpDraws=draws;
        }
        return super.tick(client,tick);
    }
    @Override protected void verifyCtrl(Minecraft client) {
        ReviewClient.require(coverage.size()==MODES.length&&shots==MODES.length&&refitViews>=MODES.length,"Incomplete live motion/gun matrix");
        var cues=NativeSoundTimeline.read(((TaczReplayChannels)film.replays.getList().get(0).keyframes).bbstacz$sounds());
        ReviewClient.require(cues.size()>MODES.length,"Moving take lost native sounds");
        System.out.println("[REVIEW motion final FP] before="+fpBefore+" now="+FirstPersonPlayback.renderedFrames()+" camera="+client.options.getCameraType());
        for(int index=0;index<MODES.length;index++) ReviewClient.require(ctrlRendered.getOrDefault(index,0)>0,"No actual Ctrl render sampled motion "+MODES[index]+": "+ctrlRendered);
        if(firstPersonReplay()) ReviewClient.require(FirstPersonPlayback.renderedFrames()>fpBefore+5,"Moving Ctrl playback did not draw native FP poses");
        else ReviewClient.require(bodyDraws>5,"Moving Ctrl playback did not draw recorded body poses");
        System.out.println("[REVIEW movement playback] fp="+firstPersonReplay()+" body draws="+bodyDraws+" native sounds="+cues.size());
    }
    @Override protected void verifyVideo(Minecraft client,Path video) throws Exception {
        double world=(ReviewProbe.lastWorldTime-ReviewProbe.firstWorldTime)/20d,gun=(ReviewProbe.lastGunTime-ReviewProbe.firstGunTime)/1e9;
        ReviewClient.require(Math.abs(world-gun)<0.02,"Moving F6 world/gun clocks disagree");
        for(int index=0;index<MODES.length;index++) ReviewClient.require(exportRendered.getOrDefault(index,0)>0,"No F6 render sampled motion "+MODES[index]+": "+exportRendered);
        writeMotionEvidence();
        System.out.println("[REVIEW movement export] first-person="+firstPersonReplay()+" world/gun="+world+"/"+gun+" body draws="+bodyDraws+" sampled motions="+exportRendered+" video="+video);
        ReviewProbe.playerDraw=null;
    }
}
