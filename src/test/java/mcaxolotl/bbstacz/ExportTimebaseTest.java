package mcaxolotl.bbstacz;

import mcaxolotl.bbstacz.recording.ExportTimebase;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExportTimebaseTest {
    @Test void videoTimeIgnoresSlowOrFastWallClockAndHeldFrames() {
        var clock=new ExportTimebase(); assertEquals(1000,clock.sample(1000,false,0));
        assertEquals(1000,clock.sample(1000,true,500));
        assertEquals(1100,clock.sample(9000,true,600));
        assertEquals(1100,clock.sample(9900,true,600));
        assertEquals(1200,clock.sample(9901,true,700));
    }
    @Test void leavingAndRestartingExportNeverRewindsNativeEpoch() {
        var clock=new ExportTimebase(); clock.sample(1000,true,0); assertEquals(6000,clock.sample(2000,true,5000));
        assertEquals(6000,clock.sample(2100,false,0)); assertEquals(6100,clock.sample(2200,false,0));
        assertEquals(6200,clock.sample(2300,true,0)); assertEquals(7200,clock.sample(2400,true,1000));
        assertEquals(7200,clock.sample(2500,false,0));
    }
    @Test void resetAllowsANewWorldEpoch() {
        var clock=new ExportTimebase(); clock.sample(1000,false,0); clock.reset(); assertEquals(0,clock.sample(0,false,0));
    }
}
