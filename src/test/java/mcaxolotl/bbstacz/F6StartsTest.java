package mcaxolotl.bbstacz;

import mcaxolotl.bbstacz.client.F6Starts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class F6StartsTest {
    @BeforeEach @AfterEach void clear() { F6Starts.clear(); }
    @Test void onlyTheCancelledResponseIsRejected() { var cancelled=F6Starts.request("film"); F6Starts.cancel(cancelled); F6Starts.request("film"); assertTrue(F6Starts.receive("film")); assertFalse(F6Starts.receive("film")); assertFalse(F6Starts.receive("film")); }
    @Test void arrivedTicketCannotRejectASeparatePlay() { var ticket=F6Starts.request("film"); assertFalse(F6Starts.receive("film")); F6Starts.cancel(ticket); assertFalse(F6Starts.receive("film")); }
    @Test void onlyAnAcceptedF6ReplyHoldsItsInitialFrame() { F6Starts.request("film"); assertFalse(F6Starts.receive("film")); assertTrue(F6Starts.holdInitialFrame("film")); assertFalse(F6Starts.holdInitialFrame("film")); assertFalse(F6Starts.receive("ordinary")); assertFalse(F6Starts.holdInitialFrame("ordinary")); var cancelled=F6Starts.request("cancelled"); F6Starts.cancel(cancelled); assertTrue(F6Starts.receive("cancelled")); assertFalse(F6Starts.holdInitialFrame("cancelled")); }
    @Test void filmIdsAndDisconnectCleanupAreIndependent() { var ticket=F6Starts.request("a"); F6Starts.cancel(ticket); F6Starts.request("b"); assertFalse(F6Starts.receive("b")); F6Starts.clear(); assertFalse(F6Starts.receive("a")); }
}
