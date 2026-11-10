package mcaxolotl.bbstacz;

import java.util.List;
import java.util.Map;
import mcaxolotl.bbstacz.recording.FrameFactory;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.PartPose;
import mcaxolotl.bbstacz.recording.PoseMath;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MotionMetadataTest {
    private FrameSnapshot frame(float swim,float glide) {
        var motion=new PartPose(new float[]{swim,glide,0,0,0,0,0,0,0,1,1,1,0,0,0,1},true,false);
        return new FrameSnapshot("",false,Map.of(),Map.of("motion",motion),PoseMath.identity(),true,false,0,0,-1,1000,0,List.of(),new float[]{0,0,0,1},List.of());
    }
    @Test void codecRetainsVanillaGlideAndSwimPhaseWithoutNewSchema() {
        var original=frame(0.7f,24); var copy=FrameFactory.INSTANCE.fromData(FrameFactory.INSTANCE.toData(original));
        assertTrue(FrameFactory.INSTANCE.compare(original,copy)); assertEquals(24,copy.humanoid().get("motion").at(1));
    }
    @Test void motionPhasesInterpolateAndOldZeroMetadataRemainsReadable() {
        var sample=frame(0.2f,20).interpolate(frame(0.8f,24),0.5f);
        assertEquals(22,sample.humanoid().get("motion").at(1)); assertEquals(0.5f,sample.humanoid().get("motion").at(0),0.00001f);
        assertEquals(0,FrameFactory.INSTANCE.fromData(FrameFactory.INSTANCE.toData(frame(0,0))).humanoid().get("motion").at(1));
    }
}
