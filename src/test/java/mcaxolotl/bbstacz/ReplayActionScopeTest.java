package mcaxolotl.bbstacz;

import java.util.concurrent.atomic.AtomicBoolean;
import mcaxolotl.bbstacz.recording.ReplayActionScope;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReplayActionScopeTest {
    @Test void nestedSceneRestorationStaysScoped() {
        assertFalse(ReplayActionScope.seeking());
        ReplayActionScope.seek(()-> { assertTrue(ReplayActionScope.seeking()); ReplayActionScope.seek(()->assertTrue(ReplayActionScope.seeking())); assertTrue(ReplayActionScope.seeking()); });
        assertFalse(ReplayActionScope.seeking());
    }
    @Test void corruptClipCannotLeaveAllFutureShotsMuted() {
        assertThrows(IllegalArgumentException.class,()->ReplayActionScope.seek(()->{throw new IllegalArgumentException("invalid clip");}));
        assertFalse(ReplayActionScope.seeking());
    }
    @Test void anotherServerClientThreadIsNotMarkedSeeking() throws Exception {
        var other=new AtomicBoolean(true);
        ReplayActionScope.seek(()-> {
            var thread=new Thread(()->other.set(ReplayActionScope.seeking())); thread.start();
            try { thread.join(); } catch (InterruptedException failure) { throw new RuntimeException(failure); }
        }); assertFalse(other.get());
    }
}
