package mcaxolotl.bbstacz;

import mcaxolotl.bbstacz.recording.NativeSoundTimeline;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeSoundRevisionTest {
    @Test void revisionChangesForInPlaceCueEditsAndStops() {
        KeyframeChannel<String> channel=new KeyframeChannel<>("sounds",mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories.STRING);
        NativeSoundTimeline.append(channel,0,NativeSoundTimeline.play("one","tacz:ak47/ak47_shoot",1,1,30,false,0,false,true,30));
        long initial=NativeSoundTimeline.revision(channel); assertEquals(initial,NativeSoundTimeline.revision(channel));
        NativeSoundTimeline.append(channel,5,NativeSoundTimeline.stop("one")); assertNotEquals(initial,NativeSoundTimeline.revision(channel));
    }
}
