package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import java.nio.file.Files;
import java.nio.file.Path;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.WorldFilmController;
import mchorse.bbs_mod.film.FrozenFilmController;
import mchorse.bbs_mod.film.WorldVideoExportSession;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Default right-Alt/right-Ctrl/F6 keys: no direct recorder apply and no test-only save after the take. */
public class HotkeyWorkflowReview implements ReviewClient.Scenario {
    protected Film film;
    protected UIFilmPanel panel;
    protected WorldVideoExportSession export;
    protected Path output;
    protected int phase, phaseTick, frames;
    private long drawnBefore;
    protected boolean ctrlFinished(Minecraft client,int elapsed) { return elapsed==ctrlTicks(); }
    protected int ctrlTicks() { return 10; }
    protected void verifyCtrl(Minecraft client) throws Exception {}
    protected void verifyVideo(Minecraft client,Path video) throws Exception {}
    protected void act(Minecraft client,int time) {
        if (time==5) GunItemRendererWrapper.INSTANCE.get().getStateMachine(client.player.getMainHandItem()).trigger("inspect");
        if (time==10) ReviewClient.require(SoundPlayManager.playClientSound(client.player,new ResourceLocation("tacz","ak47/ak47_shoot"),0.8f,0.95f,30)!=null,"Native gun sound is unavailable");
    }
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if (tick==0) {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            mchorse.bbs_mod.BBSSettings.recordingCountdown.set(0f);
            var form=new TaczForm(); Morph.getMorph(client.player).setForm(form);
            var gun=GunItemBuilder.create().setId(new ResourceLocation("tacz","ak47")).setAmmoCount(30).setAmmoInBarrel(true).build();
            client.player.setItemSlot(EquipmentSlot.MAINHAND,gun); AttachmentPropertyManager.postChangeEvent(client.player,gun);
            var uuid=client.player.getUUID(); client.getSingleplayerServer().execute(()-> { var p=client.getSingleplayerServer().getPlayerList().getPlayer(uuid); p.setItemSlot(EquipmentSlot.MAINHAND,gun.copy()); });
            film=new Film(); film.setId("bbs-tacz-hotkey-review-"+System.currentTimeMillis());
            var replay=new Replay("fp_player"); replay.form.set(form); replay.fp.set(true); film.replays.add(replay);
            // Only the initially empty film is saved, just as a user creating a new film would do.
            ContentType.FILMS.getRepository().save(film.getId(),(mchorse.bbs_mod.data.types.MapType)film.toData());
            panel=BBSModClient.getDashboard().getPanel(UIFilmPanel.class); panel.fill(film); panel.replayEditor.setReplay(replay);
            output=Path.of("review-hotkeys-"+System.currentTimeMillis()).toAbsolutePath(); Files.createDirectories(output); F4ClockReview.configure(output,30);
            var field=BBSModClient.class.getDeclaredField("worldExportSession"); field.setAccessible(true); export=(WorldVideoExportSession)field.get(null);
        }
        if (phase==0 && tick>30 && ReviewProbe.liveGunDraws>8) { F4ClockReview.key(GLFW.GLFW_KEY_RIGHT_ALT); phase=1; phaseTick=tick; }
        else if (phase==1 && BBSModClient.getFilms().getRecorder()!=null) {
            BBSModClient.getFilms().getRecorder().countdown=0; phase=2; phaseTick=tick;
        } else if (phase==2) {
            act(client,tick-phaseTick);
            if (tick-phaseTick==50) { F4ClockReview.key(GLFW.GLFW_KEY_RIGHT_ALT); phase=3; phaseTick=tick; }
        } else if (phase==3 && tick-phaseTick==20) {
            ReviewClient.require(BBSModClient.getFilms().getRecorder()==null,"Alt did not stop the take");
            var channel=((TaczReplayChannels)film.replays.getList().get(0).keyframes).bbstacz$state();
            ReviewClient.require(!channel.isEmpty()&&channel.getKeyframes().stream().anyMatch(k->k.getValue().firstPerson()),"Alt did not apply recorded first-person frames");
            int serverDuration=client.getSingleplayerServer().submit(()->BBSMod.getFilms().load(film.getId()).calculateDuration()).get();
            System.out.println("[REVIEW hotkeys] Alt client duration="+film.calculateDuration()+" server duration="+serverDuration+" audio clips="+film.camera.getClips(mchorse.bbs_mod.camera.clips.misc.AudioClip.class).size());
            client.player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY); Morph.getMorph(client.player).setForm(null);
            F4ClockReview.key(GLFW.GLFW_KEY_RIGHT_CONTROL); phase=4; phaseTick=tick;
        } else if (phase==4) {
            var controller=BBSModClient.getFilms().getController(film.getId());
            if (controller instanceof WorldFilmController && !(controller instanceof FrozenFilmController)) {
                System.out.println("[REVIEW hotkeys] Ctrl controller="+controller.getClass().getSimpleName()+" duration="+controller.film.calculateDuration());
                if (FirstPersonPlayback.target(0)!=null) { phase=5; phaseTick=tick; }
                else ReviewClient.require(tick-phaseTick<30,"Ctrl selected no recorded FP gun (tick="+controller.getTick()+")");
            } else ReviewClient.require(tick-phaseTick<60,"Ctrl failed to start world playback");
        } else if (phase==5 && ctrlFinished(client,tick-phaseTick)) {
            verifyCtrl(client); if(BBSModClient.getFilms().getController(film.getId())!=null) F4ClockReview.key(GLFW.GLFW_KEY_RIGHT_CONTROL); phase=6; phaseTick=tick;
        } else if (phase==6 && tick-phaseTick==10) {
            ReviewProbe.exportFilm=film.getId(); ReviewProbe.firstExportTick=-1; drawnBefore=FirstPersonPlayback.renderedFrames();
            F4ClockReview.key(GLFW.GLFW_KEY_F6); phase=7; phaseTick=tick;
        } else if (phase==7) {
            frames=Math.max(frames,BBSModClient.getVideoRecorder().getCounter());
            if (tick-phaseTick>30 && !export.isExporting()) {
                var movies=F4ClockReview.movies(output);
                ReviewClient.require(movies.size()==1&&Files.size(movies.get(0))>1000,"F6 hotkey exported no video after real Alt/Ctrl workflow");
                ReviewClient.require(ReviewProbe.firstExportTick==0,"F6 lost its opening frame");
                ReviewClient.require(FirstPersonPlayback.renderedFrames()>drawnBefore+5,"F6 did not render the recorded first-person gun");
                Files.writeString(Path.of("bbs-tacz-review-"+getClass().getSimpleName()+"-video.txt"),movies.get(0)+"\n");
                System.out.println("[REVIEW hotkeys] F6 frames="+frames+" video="+movies.get(0));
                verifyVideo(client,movies.get(0));
                ReviewClient.require(film.camera.getClips(mchorse.bbs_mod.camera.clips.misc.AudioClip.class).isEmpty(),"Gun sounds were extracted into BBS audio clips instead of native replay cues");
                return true;
            }
        }
        ReviewClient.require(phase<1 || tick-phaseTick<250,"Hotkey workflow stalled at phase "+phase);
        return false;
    }
}
