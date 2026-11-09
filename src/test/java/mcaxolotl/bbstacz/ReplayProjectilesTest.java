package mcaxolotl.bbstacz;

import mcaxolotl.bbstacz.recording.ReplayProjectiles;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReplayProjectilesTest {
    @Test void directActionsWithoutAFilmAreNotAnOwnedReplayLifetime() {
        assertDoesNotThrow(() -> ReplayProjectiles.add(null,null));
        assertDoesNotThrow(() -> ReplayProjectiles.clear(null));
        assertDoesNotThrow(ReplayProjectiles::clearAll);
    }
}
