package mcaxolotl.bbstacz.smoke;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mcaxolotl.bbstacz.client.PlaybackContext;
import mcaxolotl.bbstacz.client.RecordedAudio;
import mcaxolotl.bbstacz.config.TaczConfig;
import mcaxolotl.bbstacz.recording.NativeSoundTimeline;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.minecraft.client.Minecraft;

/** Actual editor rendering: clear/re-add cues, POV/FP/config switches, multi-actor removal and teardown. */
public final class EditorAudioModesReview implements ReviewClient.Scenario {
    private UIFilmPanel panel;
    private Replay replay,other;
    private int phase,start;
    private final List<Map<String,Object>> evidence=new ArrayList<>();
    private void next(int tick,String check) { evidence.add(Map.of("check",check,"status","PASS")); phase++; start=tick; }
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick==0) {
            var film=AudioModeFixture.moving(client,true,false);
            film.camera.get().get(0).duration.set(500);
            replay=film.replays.getList().get(0);
            var sounds=((TaczReplayChannels)replay.keyframes).bbstacz$sounds(); sounds.removeAll();
            NativeSoundTimeline.append(sounds,0,NativeSoundTimeline.play("long","tacz:ak47/ak47_reload_mvt",0.8f,1,100,false,0,false,true,450));
            var dashboard=BBSModClient.getDashboard(); panel=dashboard.getPanel(UIFilmPanel.class); panel.fill(film);
            dashboard.getPanels().setPanel(panel); UIScreen.open(dashboard); panel.replayEditor.setReplay(replay);
            panel.getController().createEntities(); panel.getController().setPov(UIFilmController.CAMERA_MODE_FIRST_PERSON);
            panel.getRunner().setWork(film.camera); panel.getRunner().setCursor(0); panel.getRunner().setPlaying(true);
        }
        BaseFilmController controller=panel.getController().editorController;
        var sounds=((TaczReplayChannels)replay.keyframes).bbstacz$sounds();
        var voices=RecordedAudio.voices(controller);
        if(phase==0&&tick>15&&!voices.isEmpty()) {
            ReviewClient.require(PlaybackContext.firstPerson(controller,replay)&&voices.get(0).isRelative(),"Editor FP voice is not relative to the selected first-person replay");
            sounds.removeAll(); next(tick,"editor first-person voice");
        } else if(phase==1&&tick-start>=12) {
            ReviewClient.require(voices.isEmpty(),"Clearing the entire native sound channel left stale editor voices playing");
            NativeSoundTimeline.append(sounds,0,NativeSoundTimeline.play("restored","tacz:ak47/ak47_reload_mvt",0.8f,1,100,false,0,false,true,450));
            next(tick,"clear whole sound channel");
        } else if(phase==2&&tick-start>=12&&!voices.isEmpty()) {
            panel.getController().setPov(UIFilmController.CAMERA_MODE_THIRD_PERSON_BACK); next(tick,"restore edited channel");
        } else if(phase==3&&tick-start>=12) {
            ReviewClient.require(!voices.isEmpty()&&!PlaybackContext.firstPerson(controller,replay)&&!voices.get(0).isRelative(),"Switching editor POV did not rebuild a spatial actor voice");
            panel.getController().setPov(UIFilmController.CAMERA_MODE_FIRST_PERSON); TaczConfig.INSTANCE.firstPersonReplay=false; next(tick,"editor FP to TP POV");
        } else if(phase==4&&tick-start>=12) {
            ReviewClient.require(!voices.isEmpty()&&!voices.get(0).isRelative(),"Disabling the FP config silenced/misplaced editor audio");
            TaczConfig.INSTANCE.firstPersonReplay=true; next(tick,"first-person config off");
        } else if(phase==5&&tick-start>=12) {
            ReviewClient.require(!voices.isEmpty()&&voices.get(0).isRelative(),"Re-enabling FP config did not restore the relative voice");
            replay.fp.set(false); next(tick,"first-person config on");
        } else if(phase==6&&tick-start>=12) {
            ReviewClient.require(!voices.isEmpty()&&!voices.get(0).isRelative(),"Removing replay FP flag silenced native actor audio");
            other=new Replay("second_sound_actor"); other.form.set(new mcaxolotl.bbstacz.forms.TaczForm());
            other.keyframes.x.insert(0,client.player.getX()+20); other.keyframes.y.insert(0,client.player.getY()); other.keyframes.z.insert(0,client.player.getZ());
            ((TaczReplayChannels)other.keyframes).bbstacz$state().insert(0,mcaxolotl.bbstacz.recording.FrameSnapshot.EMPTY);
            NativeSoundTimeline.append(((TaczReplayChannels)other.keyframes).bbstacz$sounds(),0,NativeSoundTimeline.play("other","tacz:ak47/ak47_reload_mvt",0.8f,1,100,false,0,false,true,450));
            controller.film.replays.add(other); panel.getController().createEntities(); next(tick,"replay FP flag off");
        } else if(phase==7&&tick-start>=12) {
            ReviewClient.require(voices.size()==2,"Two actors did not own two separate native voices: "+voices.size());
            ReviewClient.require(Math.abs(voices.get(0).getX()-voices.get(1).getX())>5,"Two actor voices shared one position");
            controller.film.replays.remove(other); next(tick,"multiple moving/spatial actors");
        } else if(phase==8&&tick-start>=12) {
            ReviewClient.require(voices.size()==1,"Deleting an actor retained its cached native voice");
            client.setScreen(null); next(tick,"remove actor during playback");
        } else if(phase==9&&tick-start>=12) {
            ReviewClient.require(voices.isEmpty(),"Closing editor leaked native actor voices");
            evidence.add(Map.of("check","close editor","status","PASS"));
            java.nio.file.Files.writeString(java.nio.file.Path.of("bbs-tacz-editor-audio-modes.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(evidence));
            System.out.println("[REVIEW editor audio modes] "+evidence); return true;
        }
        ReviewClient.require(tick<420,"Editor audio modes stalled at phase "+phase); return false;
    }
}
