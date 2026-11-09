package mcaxolotl.bbstacz;

import mcaxolotl.bbstacz.client.F6FrameGate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class F6FrameGateTest {
    @AfterEach void clear() { F6FrameGate.clear(); }
    @Test void timelineIsReleasedOnlyOnceOnCapture() { var count=new AtomicInteger(); F6FrameGate.await(new Object(),count::incrementAndGet); assertEquals(0,count.get()); F6FrameGate.captured(); F6FrameGate.captured(); assertEquals(1,count.get()); }
    @Test void unrelatedCancellationDoesNotReleaseOrRemoveTheGate() { var count=new AtomicInteger(); Object owner=new Object(); F6FrameGate.await(owner,count::incrementAndGet); F6FrameGate.cancel(new Object()); F6FrameGate.captured(); assertEquals(1,count.get()); }
    @Test void cancelledOrDisconnectedGateNeverResumesPlayback() { var count=new AtomicInteger(); Object owner=new Object(); F6FrameGate.await(owner,count::incrementAndGet); F6FrameGate.cancel(owner); F6FrameGate.captured(); assertEquals(0,count.get()); F6FrameGate.await(owner,count::incrementAndGet); F6FrameGate.clear(); F6FrameGate.captured(); assertEquals(0,count.get()); }
}
