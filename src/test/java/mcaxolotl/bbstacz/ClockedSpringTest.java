package mcaxolotl.bbstacz;

import mcaxolotl.bbstacz.recording.ClockedSpring;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClockedSpringTest {
    @Test void renderFrequencyCannotChangeTheSpringResult() {
        var coarse=new ClockedSpring(3,0.5f,0,0,0); var fine=new ClockedSpring(3,0.5f,0,0,0);
        coarse.update(1,0,false); fine.update(1,0,false);
        for (long t=1_000_000;t<=90_000_000;t+=1_000_000) fine.get(t,false);
        assertEquals(fine.get(90_000_000,false),coarse.get(90_000_000,false));
    }
    @Test void heldOrPausedFramesCannotAdvanceOrDoubleStep() {
        var spring=new ClockedSpring(3,0.5f,0,0,0); spring.update(1,0,false);
        float output=spring.get(60_000_000,false);
        for(int i=0;i<100;i++) assertEquals(output,spring.get(60_000_000,false));
        assertEquals(output,spring.get(120_000_000,true));
    }
    @Test void targetSwitchIntegratesTheOldTargetNotTheFutureOne() {
        var spring=new ClockedSpring(3,0.5f,0,0,0); var other=new ClockedSpring(3,0.5f,0,0,0);
        spring.update(1,0,false); other.update(1,0,false);
        float expected=other.get(60_000_000,false); assertEquals(expected,spring.update(-1,60_000_000,false));
        assertEquals(expected,spring.get(60_000_000,false));
        assertTrue(Float.isFinite(spring.get(10_000_000_000L,false)));
    }
}
