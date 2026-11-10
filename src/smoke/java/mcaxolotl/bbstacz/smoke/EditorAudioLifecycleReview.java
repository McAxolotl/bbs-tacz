package mcaxolotl.bbstacz.smoke;

import mcaxolotl.bbstacz.client.RecordedAudio;
import mcaxolotl.bbstacz.recording.NativeSoundTimeline;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.minecraft.client.Minecraft;

/** Editor runner play/pause/seek, track off/on, cue editing and hiding the live editor. */
public final class EditorAudioLifecycleReview implements ReviewClient.Scenario {
    private UIFilmPanel panel; private int phase,start;
    @Override public boolean tick(Minecraft client,int tick) {
        if(tick==0) {
            var film=AudioModeFixture.moving(client,false); var dashboard=BBSModClient.getDashboard(); panel=dashboard.getPanel(UIFilmPanel.class); panel.fill(film); dashboard.getPanels().setPanel(panel); UIScreen.open(dashboard); panel.getController().createEntities(); panel.getRunner().setWork(film.camera); panel.getRunner().setCursor(5); panel.getRunner().setPlaying(true);
        }
        var controller=panel.getController().editorController; var replay=controller.film.replays.getList().get(0);
        if(phase==0&&tick>10&&!RecordedAudio.voices(controller).isEmpty()) { panel.getRunner().setPlaying(false); replay.enabled.set(false); phase=1; start=tick; }
        if(phase==1&&tick-start>=2) { ReviewClient.require(RecordedAudio.voices(controller).isEmpty(),"Disabling editor actor kept native voices alive"); replay.enabled.set(true); panel.getRunner().setPlaying(true); phase=2; start=tick; }
        if(phase==2&&tick-start>=3) {
            ReviewClient.require(!RecordedAudio.voices(controller).isEmpty(),"Re-enabling editor actor did not reconstruct its active native voice");
            panel.getRunner().setPlaying(false); var sounds=((TaczReplayChannels)replay.keyframes).bbstacz$sounds();
            for(var cue:NativeSoundTimeline.read(sounds)) NativeSoundTimeline.append(sounds,panel.getRunner().ticks,NativeSoundTimeline.stop(cue.id()));
            phase=3; start=tick;
        }
        if(phase==3&&tick-start>=2) { ReviewClient.require(RecordedAudio.voices(controller).isEmpty(),"Editing native stop cues left stale cached sounds playing"); panel.getRunner().setCursor(2); panel.getRunner().setPlaying(true); phase=4; start=tick; }
        if(phase==4&&tick-start>=3) { ReviewClient.require(!RecordedAudio.voices(controller).isEmpty(),"Backwards editor seek did not restore native sound"); client.setScreen(null); phase=5; start=tick; }
        if(phase==5&&tick-start>=2) { ReviewClient.require(RecordedAudio.voices(controller).isEmpty(),"Hidden editor continued playing native sounds"); System.out.println("[REVIEW editor lifecycle] actor toggle, cue edit, backward seek and hidden-editor stop PASS"); return true; }
        ReviewClient.require(tick<150,"Editor audio lifecycle fixture stalled at "+phase); return false;
    }
}
