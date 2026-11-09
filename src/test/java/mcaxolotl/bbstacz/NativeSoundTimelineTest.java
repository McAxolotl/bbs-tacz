package mcaxolotl.bbstacz;

import mcaxolotl.bbstacz.recording.NativeSoundTimeline;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeSoundTimelineTest {
    private KeyframeChannel<String> channel() { return new KeyframeChannel<>("bbs_tacz:sounds",KeyframeFactories.STRING); }
    @Test void preserveRandomizedNativeParametersAndFractionalStop() {
        var sounds=channel(); NativeSoundTimeline.append(sounds,3.25f,NativeSoundTimeline.play("take:1","tacz:ak47/ak47_shoot",0.72f,0.97f,32,false,6,true,false,40));
        NativeSoundTimeline.append(sounds,8.75f,NativeSoundTimeline.stop("take:1"));
        var cue=NativeSoundTimeline.read(sounds).get(0); assertEquals(3.25f,cue.tick()); assertEquals(8.75f,cue.end());
        assertEquals(0.72f,cue.volume()); assertEquals(0.97f,cue.pitch()); assertTrue(cue.tracking()); assertFalse(cue.relative());
        assertFalse(sounds.getKeyframes().get(0).getValue().contains(".wav"));
    }
    @Test void multipleCommandsAtTheSameTimeDoNotOverwriteEachOther() {
        var sounds=channel(); NativeSoundTimeline.append(sounds,1,NativeSoundTimeline.play("take:1","tacz:ak47/ak47_shoot",1,1,30,false,0,false,true,4));
        NativeSoundTimeline.append(sounds,1,NativeSoundTimeline.play("take:2","tacz:ak47/ak47_reload_mvt",1,1,30,false,0,false,true,30));
        assertEquals(2,NativeSoundTimeline.read(sounds).size());
        assertEquals(5,NativeSoundTimeline.read(sounds).get(0).end());
    }
    @Test void invalidAndFutureSchemaBatchesCannotCrashPlayback() {
        var sounds=channel(); sounds.insert(0,"corrupt JSON"); sounds.insert(1,"[{\"v\":2}]");
        NativeSoundTimeline.append(sounds,2,NativeSoundTimeline.play("good","tacz:ak47/ak47_shoot",1,1,30,false,0,false,true,4));
        assertEquals(1,NativeSoundTimeline.read(sounds).size());
    }
}
