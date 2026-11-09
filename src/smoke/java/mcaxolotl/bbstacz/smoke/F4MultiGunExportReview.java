package mcaxolotl.bbstacz.smoke;

import java.nio.file.Files;
import java.nio.file.Path;
import mcaxolotl.bbstacz.forms.TaczForm;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** The same five-gun animation combination matrix performed live while actual F4 records video/native audio. */
public final class F4MultiGunExportReview extends MultiGunWorkflowReview {
    private Path output;
    private int stage,start,frames;
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick==0) {
            client.options.setCameraType(CameraType.FIRST_PERSON); Morph.getMorph(client.player).setForm(new TaczForm());
            output=Path.of("review-f4-combinations-"+System.currentTimeMillis()).toAbsolutePath(); Files.createDirectories(output); F4ClockReview.configure(output,30);
        }
        if(stage==0&&tick>25) { F4ClockReview.key(GLFW.GLFW_KEY_F4); ReviewProbe.resetClock(); stage=1; }
        if(stage==1&&BBSModClient.getVideoRecorder().isRecording()) { stage=2; start=tick; ProjectileProbe.phase="f4"; }
        if(stage==2) {
            act(client,tick-start); frames=Math.max(frames,BBSModClient.getVideoRecorder().getCounter());
            if(tick-start>=recordTicks()) { F4ClockReview.key(GLFW.GLFW_KEY_F4); stage=3; }
        }
        if(stage==3&&!BBSModClient.getVideoRecorder().isRecording()) {
            var movies=F4ClockReview.movies(output); ReviewClient.require(movies.size()==1&&Files.size(movies.get(0))>1000,"F4 multi-gun recording exported no video");
            ReviewClient.require(ProjectileProbe.draws("f4")>0&&ProjectileProbe.duplicateDraws==0,"F4 captured tracers were missing or drawn twice per frame");
            ReviewClient.require(successfulWeapons()==GUNS.length,"F4 did not fire every real gun class");
            ReviewClient.require(ReviewProbe.serverSuccessfulShots>=GUNS.length,"F4 client success did not produce real server firing");
            var sounds=BBSModClient.getMinecraftSoundCapture().getSounds();
            ReviewClient.require(sounds.stream().filter(s->s.location.getPath().startsWith("tacz_sounds/")).count()>=GUNS.length,"F4 did not capture the guns' native resources");
            double world=(ReviewProbe.lastWorldTime-ReviewProbe.firstWorldTime)/20d,gun=(ReviewProbe.lastGunTime-ReviewProbe.firstGunTime)/1e9;
            ReviewClient.require(Math.abs(world-gun)<0.02,"F4 multi-gun/world clocks disagree");
            var probe=new ProcessBuilder("/usr/bin/ffprobe","-v","error","-show_entries","stream=codec_name","-of","csv=p=0",movies.get(0).toString()).start();
            String codecs=new String(probe.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            ReviewClient.require(probe.waitFor()==0&&codecs.contains("h264")&&codecs.contains("aac"),"F4 multi-gun video lacks native audio/video streams");
            Files.writeString(Path.of("bbs-tacz-review-F4MultiGunExportReview-video.txt"),movies.get(0)+"\n");
            System.out.println("[REVIEW F4 combinations] real guns="+successfulWeapons()+" server shots="+ReviewProbe.serverSuccessfulShots+" sounds="+sounds.size()+" world/gun seconds="+world+"/"+gun+" frames="+frames+" tracer draws="+ProjectileProbe.draws("f4")+" duplicates="+ProjectileProbe.duplicateDraws+" video="+movies.get(0));
            return true;
        }
        return false;
    }
}
