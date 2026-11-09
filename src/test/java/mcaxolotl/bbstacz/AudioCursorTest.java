package mcaxolotl.bbstacz;

import mcaxolotl.bbstacz.recording.AudioCursor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioCursorTest {
    @Test void pausingTheSameTickMustNotRecreateOrStopTheVoice() {
        assertFalse(AudioCursor.seeking(21.9f, 21, true, false, false));
    }
    @Test void resumingTheSameTickIsNotASeek() {
        assertFalse(AudioCursor.seeking(21, 21.9f, false, true, false));
    }
    @Test void aRealBackwardsTickStillSeeksWhilePausing() {
        assertTrue(AudioCursor.seeking(22.9f, 21, true, false, false));
    }
    @Test void pausedAndPlayingBackwardsScrubsStillSeek() {
        assertTrue(AudioCursor.seeking(21.9f, 21, false, false, false));
        assertTrue(AudioCursor.seeking(21.9f, 21, true, true, false));
    }
    @Test void videoLowFpsStepsAreNotForwardScrubs() {
        assertTrue(AudioCursor.seeking(20, 30, true, true, false));
        assertFalse(AudioCursor.seeking(20, 30, true, true, true));
    }
}
