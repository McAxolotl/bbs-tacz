package mcaxolotl.bbstacz.smoke;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import mcaxolotl.bbstacz.client.RecordedAudio;
import mcaxolotl.bbstacz.recording.NativeSoundTimeline;
import mcaxolotl.bbstacz.recording.ReplayBindings;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.data.storage.DataFileStorage;
import mchorse.bbs_mod.data.storage.DataGzipStorage;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.WorldVideoExportSession;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Export the complete real Alt-recorded six-motion TP film in a fresh client, without preceding GPU previews.
 * This is also an old-film serialization/reopen regression; it never substitutes a synthetic pose/movie. */
public class RecordedThirdPersonF6Review implements ReviewClient.Scenario {
    private Film film; private WorldVideoExportSession export; private Path output; private long audio;
    private final HashMap<Integer,Integer> rendered=new HashMap<>();
    protected boolean firstPerson() { return false; }
    protected String sourceScenario() { return "MovingThirdPersonWorkflowReview"; }
    @Override public int timeoutTicks() { return 2400; }
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick==0) {
            Path input;
            try(var files=Files.walk(Path.of("saves"))) {
                input=files.filter(p->p.toString().contains("bbs-tacz-review-"+sourceScenario()+"-")
                    &&p.getParent().endsWith("films")&&p.getFileName().toString().startsWith("bbs-tacz-hotkey-review-")&&p.toString().endsWith(".dat"))
                    .max(Comparator.comparingLong(p->p.toFile().lastModified())).orElseThrow(()->new AssertionError("Run "+sourceScenario()+" to record a real six-motion film first"));
            }
            film=new Film(); film.fromData(new DataGzipStorage(new DataFileStorage(input.toFile())).read()); film.setId("bbs-tacz-reopened-tp-"+System.currentTimeMillis());
            var channels=(TaczReplayChannels)film.replays.getList().get(0).keyframes;
            ReviewClient.require(film.calculateDuration()>=1200&&film.hasFirstPerson()==firstPerson()&&channels.bbstacz$state().getKeyframes().size()>=(firstPerson()?6:1200),"Reopened film is not the complete real motion take");
            ReviewClient.require(NativeSoundTimeline.read(channels.bbstacz$sounds()).size()>50,"Reopened film lost native sounds");
            ContentType.FILMS.getRepository().save(film.getId(),film.toData().asMap()); BBSModClient.getDashboard().getPanel(UIFilmPanel.class).fill(film);
            output=Path.of("review-reopened-tp-f6-"+System.currentTimeMillis()).toAbsolutePath(); Files.createDirectories(output); F4ClockReview.configure(output,30);
            var field=BBSModClient.class.getDeclaredField("worldExportSession"); field.setAccessible(true); export=(WorldVideoExportSession)field.get(null);
            ReviewProbe.exportFilm=film.getId(); ReviewProbe.resetClock(); audio=RecordedAudio.playedCues(); ProjectileProbe.phase="reopened_f6";
            if(firstPerson()) {
                client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                ReviewProbe.recordedGunDraw=gun->{var target=mcaxolotl.bbstacz.client.FirstPersonPlayback.target(0); if(target!=null&&target.tick()<1200&&BBSModClient.getVideoRecorder().isRecording()) rendered.merge(Math.max(0,(int)target.tick()/200),1,Integer::sum);};
            } else ReviewProbe.playerDraw=draw->{ var binding=ReplayBindings.get(draw.scope().entity); if(binding!=null&&binding.tick()<1200&&draw.scope().frame!=null&&!draw.scope().frame.humanoid().isEmpty()) {
                var arm=draw.scope().frame.humanoid().get("right_arm"); ReviewClient.require(ReviewProbe.armError(draw,draw.scope().frame.humanoid())<0.07,"Reopened TP draw differs from its baked native arm pose");
                if(BBSModClient.getVideoRecorder().isRecording()&&binding.tick()>2) ReviewProbe.requireFramed(client,draw.scope().entity);
                rendered.merge(Math.max(0,(int)binding.tick()/200),1,Integer::sum);
            }};
            System.out.println("[REVIEW reopened film] fp="+firstPerson()+" source="+input.toAbsolutePath()+" duration="+film.calculateDuration()+" frames="+channels.bbstacz$state().getKeyframes().size());
        }
        if(tick==20) F4ClockReview.key(GLFW.GLFW_KEY_F6);
        if(tick>80&&!export.isExporting()) {
            var movies=F4ClockReview.movies(output); ReviewClient.require(movies.size()==1&&Files.size(movies.get(0))>1000,"Reopened TP F6 exported no movie");
            for(int motion=0;motion<6;motion++) ReviewClient.require(rendered.getOrDefault(motion,0)>0,"Reopened TP F6 missed motion "+motion+": "+rendered);
            ReviewClient.require(ReviewProbe.firstExportTick==0,"Reopened TP F6 dropped its opening frame");
            ReviewClient.require(RecordedAudio.playedCues()>audio+50,"Reopened TP F6 did not emit native gun sounds");
            double world=(ReviewProbe.lastWorldTime-ReviewProbe.firstWorldTime)/20d,gun=(ReviewProbe.lastGunTime-ReviewProbe.firstGunTime)/1e9;
            ReviewClient.require(world>60&&Math.abs(world-gun)<0.02,"Reopened TP F6 world/gun clocks differ: "+world+"/"+gun);
            ReviewClient.require(ProjectileProbe.duplicateDraws==0,"Reopened TP F6 duplicated native tracers");
            Files.writeString(Path.of("bbs-tacz-review-"+getClass().getSimpleName()+"-video.txt"),movies.get(0)+"\n");
            System.out.println("[REVIEW reopened F6] fp="+firstPerson()+" motions="+rendered+" native sounds="+(RecordedAudio.playedCues()-audio)+" clocks="+world+"/"+gun+" video="+movies.get(0)); ReviewProbe.playerDraw=null; ReviewProbe.recordedGunDraw=null; return true;
        }
        return false;
    }
}
