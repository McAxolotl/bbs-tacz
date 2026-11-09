package mcaxolotl.bbstacz.smoke;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.lwjgl.glfw.GLFW;

/** A real F4 export compares the gun clock at captured frames with the world's fixed-step clock. */
public final class F4ClockReview implements ReviewClient.Scenario {
    private Path output;
    private boolean started, stopping;
    private int frames;
    public static void configure(Path output, int fps) {
        BBSSettings.videoEncoderPath.set("/usr/bin/ffmpeg"); BBSSettings.videoExportPath.set(output.toString());
        BBSSettings.videoWidth.set(320); BBSSettings.videoHeight.set(180); BBSSettings.videoFrameRate.set(fps);
        BBSSettings.videoDelay.set(0f); BBSSettings.worldExportResizeWindow.set(true);
        BBSSettings.videoExportAudio.set(true); BBSSettings.videoExportMinecraftSounds.set(true);
        BBSSettings.videoMotionBlur.set(0); BBSSettings.videoHeldFrames.set(2); BBSSettings.videoLimitFrameRate.set(false);
        BBSSettings.videoOpenFolderAfterExport.set(false); BBSSettings.videoPlaySoundAfterExport.set(false);
    }
    public static void key(int code) { KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(code)); }
    public static List<Path> movies(Path output) throws Exception {
        try (var files=Files.list(output)) { return files.filter(p->p.toString().endsWith(".mp4")).toList(); }
    }
    @Override public boolean tick(Minecraft client, int tick) throws Exception {
        if (tick==0) {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            var gun=GunItemBuilder.create().setId(new ResourceLocation("tacz","ak47")).setAmmoCount(30).setAmmoInBarrel(true).build();
            client.player.setItemSlot(EquipmentSlot.MAINHAND,gun); AttachmentPropertyManager.postChangeEvent(client.player,gun);
            output=Path.of("review-f4-clock-"+System.currentTimeMillis()).toAbsolutePath(); Files.createDirectories(output);
            configure(output,10);
        }
        if (!started && tick>25 && ReviewProbe.liveGunDraws>5) { started=true; ReviewProbe.resetClock(); key(GLFW.GLFW_KEY_F4); }
        if (started) frames=Math.max(frames,BBSModClient.getVideoRecorder().getCounter());
        if (!stopping && frames>=25) { stopping=true; key(GLFW.GLFW_KEY_F4); }
        if (stopping && !BBSModClient.getVideoRecorder().isRecording()) {
            var movies=movies(output); ReviewClient.require(movies.size()==1 && Files.size(movies.get(0))>1000,"F4 did not export a video");
            double world=(ReviewProbe.lastWorldTime-ReviewProbe.firstWorldTime)/20d;
            double gun=(ReviewProbe.lastGunTime-ReviewProbe.firstGunTime)/1e9;
            System.out.println("[REVIEW F4 clock] world seconds="+world+" gun seconds="+gun+" frames="+frames+" video="+movies.get(0));
            Files.writeString(Path.of("bbs-tacz-review-F4ClockReview-video.txt"),movies.get(0)+"\n");
            ReviewClient.require(world>1 && Math.abs(world-gun)<0.02,"TACZ's clock does not follow F4's world/video clock: world="+world+" gun="+gun);
            return true;
        }
        return false;
    }
}
