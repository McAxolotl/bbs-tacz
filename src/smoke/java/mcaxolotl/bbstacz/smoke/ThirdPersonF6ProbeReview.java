package mcaxolotl.bbstacz.smoke;

import java.nio.file.Files;
import java.nio.file.Path;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.WorldVideoExportSession;
import mchorse.bbs_mod.film.WorldFilmController;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class ThirdPersonF6ProbeReview implements ReviewClient.Scenario {
    private Film film; private WorldVideoExportSession export; private Path output;
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick==0) {
            film=AudioModeFixture.moving(client,false); mchorse.bbs_mod.ui.ContentType.FILMS.getRepository().save(film.getId(),film.toData().asMap()); BBSModClient.getDashboard().getPanel(UIFilmPanel.class).fill(film);
            output=Path.of("review-tp-f6-probe-"+System.currentTimeMillis()).toAbsolutePath(); Files.createDirectories(output); F4ClockReview.configure(output,30);
            var field=BBSModClient.class.getDeclaredField("worldExportSession"); field.setAccessible(true); export=(WorldVideoExportSession)field.get(null);
        }
        if(tick==20) F4ClockReview.key(GLFW.GLFW_KEY_F6);
        if(tick>20&&tick%20==0) {
            var current=BBSModClient.getFilms().getController(film.getId());
            int serverTick=client.getSingleplayerServer().submit(()->{var a=mchorse.bbs_mod.BBSMod.getActions().getPlayer(film.getId());return a==null?-1:a.tick;}).get();
            System.out.println("[REVIEW TP F6 clock] elapsed="+tick+" exporting="+export.isExporting()+" video="+BBSModClient.getVideoRecorder().isRecording()+" counter="+BBSModClient.getVideoRecorder().getCounter()+" client="+(current==null?-1:current.getTick())+" paused="+(current==null?null:current.paused)+" server="+serverTick+" camera="+BBSModClient.getCameraController().getCurrent().getClass().getSimpleName());
        }
        if(tick>65&&!export.isExporting()) { ReviewClient.require(F4ClockReview.movies(output).size()==1,"TP F6 exported no movie"); System.out.println("[REVIEW TP F6] PASS"); return true; }
        ReviewClient.require(tick<600,"TP F6 probe stalled"); return false;
    }
}
