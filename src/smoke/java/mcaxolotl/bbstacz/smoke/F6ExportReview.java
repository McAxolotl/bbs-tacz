package mcaxolotl.bbstacz.smoke;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FrozenFilmController;
import mchorse.bbs_mod.film.WorldFilmController;
import mchorse.bbs_mod.film.WorldVideoExportSession;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Real public take -> editor apply -> save/load -> frozen preview -> actual F6 key -> FFmpeg world export. */
public final class F6ExportReview implements ReviewClient.Scenario {
    private Film film;
    private Path output;
    private WorldVideoExportSession export;
    private long renderedBefore;
    private int videoFrames;
    @Override public boolean tick(Minecraft client, int tick) throws Exception {
        if(tick==0) {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            var form=new TaczForm(); Morph.getMorph(client.player).setForm(form);
            var gun=GunItemBuilder.create().setId(new ResourceLocation("tacz","ak47")).setAmmoCount(30).setAmmoInBarrel(true).build();
            client.player.setItemSlot(EquipmentSlot.MAINHAND,gun); AttachmentPropertyManager.postChangeEvent(client.player,gun);
            film=new Film(); film.setId("bbs-tacz-f6-review-"+System.currentTimeMillis());
            var replay=new Replay("fp_player"); replay.form.set(form); replay.fp.set(true); film.replays.add(replay);
            ContentType.FILMS.getRepository().save(film.getId(),(mchorse.bbs_mod.data.types.MapType)film.toData());
            output=Path.of("review-f6-"+System.currentTimeMillis()).toAbsolutePath(); Files.createDirectories(output);
            BBSSettings.videoEncoderPath.set("/usr/bin/ffmpeg"); BBSSettings.videoExportPath.set(output.toString());
            BBSSettings.videoWidth.set(320); BBSSettings.videoHeight.set(180); BBSSettings.videoFrameRate.set(30);
            BBSSettings.videoDelay.set(0.8f); BBSSettings.worldExportResizeWindow.set(true);
            BBSSettings.videoExportAudio.set(false); BBSSettings.videoExportMinecraftSounds.set(false);
            BBSSettings.videoMotionBlur.set(0); BBSSettings.videoHeldFrames.set(1);
            BBSSettings.videoOpenFolderAfterExport.set(false); BBSSettings.videoPlaySoundAfterExport.set(false);
            var field=BBSModClient.class.getDeclaredField("worldExportSession"); field.setAccessible(true); export=(WorldVideoExportSession)field.get(null);
        }
        if(tick==20) { BBSModClient.getFilms().startRecording(film,0,0,false); BBSModClient.getFilms().getRecorder().countdown=0; }
        if(tick==15 || tick==25) {
            var machine=GunItemRendererWrapper.INSTANCE.get().getStateMachine(client.player.getMainHandItem());
            if(machine!=null) machine.trigger("inspect");
        }
        if(tick==65) {
            var recorder=BBSModClient.getFilms().stopRecording();
            var panel=BBSModClient.getDashboard().getPanel(UIFilmPanel.class); panel.fill(film); panel.applyRecordedKeyframes(recorder,film);
            Film saved=new Film(); saved.fromData(film.toData()); saved.setId(film.getId()); film=saved;
            var states=((TaczReplayChannels)film.replays.getList().get(0).keyframes).bbstacz$state();
            long fp=states.getKeyframes().stream().filter(k->k.getValue().firstPerson()).count();
            System.out.println("[REVIEW F6] saved FP keys="+fp+" duration="+film.calculateDuration());
            ReviewClient.require(fp>5,"Native FP recording did not survive the editor's real apply/save path");
            ContentType.FILMS.getRepository().save(film.getId(),(mchorse.bbs_mod.data.types.MapType)film.toData()); panel.fill(film);
            BBSModClient.getFilms().freeze(film,20,false);
            client.player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY); Morph.getMorph(client.player).setForm(null);
        }
        if(tick==85) {
            BBSModClient.getFilms().freeze(film,20,false);
            renderedBefore=FirstPersonPlayback.renderedFrames();
            System.out.println("[REVIEW F6] before key: server="+mchorse.bbs_mod.network.ClientNetwork.isIsBBSModOnServer()+" controllers="+BBSModClient.getFilms().getControllers().stream().map(c->c.getClass().getSimpleName()+":"+c.getTick()).toList());
            KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F6));
        }
        if(tick==97) {
            var controllers=BBSModClient.getFilms().getControllers();
            System.out.println("[REVIEW F6] exporting="+export.isExporting()+" managed="+controllers.stream().map(c->c.getClass().getSimpleName()+":"+c.getTick()).toList()+" target="+(FirstPersonPlayback.target(0)==null?"null":FirstPersonPlayback.target(0).controller().getClass().getSimpleName()));
            var controller=BBSModClient.getFilms().getController(film.getId());
            ReviewClient.require(export.isExporting(),"F6 cancelled before capturing the FP film");
            ReviewClient.require(controller instanceof WorldFilmController && !(controller instanceof FrozenFilmController),"F6 warm-up selected the frozen editor controller instead of the playback");
            ReviewClient.require(FirstPersonPlayback.target(0)!=null && FirstPersonPlayback.target(0).controller()==controller,"Editor preview stole the world FP replay");
        }
        if (tick==98) {
            var preview=new mchorse.bbs_mod.film.FirstPersonFilmController(film); preview.tick=20; preview.paused=true;
            FirstPersonPlayback.watch(preview);
            ReviewClient.require(FirstPersonPlayback.target(0).controller()==BBSModClient.getFilms().getController(film.getId()), "A later preview event replaced the managed world FP owner");
            preview.shutdown();
        }
        videoFrames=Math.max(videoFrames,BBSModClient.getVideoRecorder().getCounter());
        if(tick>120 && !export.isExporting()) {
            List<Path> movies;
            try(var files=Files.list(output)) { movies=files.filter(p->p.toString().endsWith(".mp4")).toList(); }
            ReviewClient.require(movies.size()==1 && Files.size(movies.get(0))>1000,"F6 produced no playable video");
            ReviewClient.require(videoFrames>15,"F6 recorded too few frames: "+videoFrames);
            ReviewClient.require(FirstPersonPlayback.renderedFrames()>renderedBefore+10,"F6 video did not render the recorded FP gun");
            Files.writeString(Path.of("bbs-tacz-review-F6ExportReview-video.txt"),movies.get(0).toString()+"\n");
            System.out.println("[REVIEW F6] video="+movies.get(0)+" frames="+videoFrames+" FP draws="+(FirstPersonPlayback.renderedFrames()-renderedBefore));
            return true;
        }
        return false;
    }
}
