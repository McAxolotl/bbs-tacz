package mcaxolotl.bbstacz;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ScheduledFuture;
import mcaxolotl.bbstacz.recording.ClockedTaskQueue;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClockedTaskQueueTest {
    @Test void stalledRenderingDoesNotCollapseBurstTimestamps() {
        var now=new AtomicLong(); var queue=new ClockedTaskQueue(now::get); var seen=new ArrayList<Long>();
        var future=new AtomicReference<ScheduledFuture<?>>();
        future.set(queue.schedule(()-> { seen.add(now.get()); if(seen.size()==3) future.get().cancel(false); },0,66,TimeUnit.MILLISECONDS));
        queue.pump(200_000_000L,(time,runnable)->{ now.set(time); runnable.run(); });
        assertEquals(java.util.List.of(0L,66_000_000L,132_000_000L),seen); assertTrue(future.get().isDone());
        queue.pump(400_000_000L,(time,runnable)->fail("Cancelled burst ran again"));
    }
    @Test void heldFramesAndCancellationDoNotRunAnotherShot() {
        var queue=new ClockedTaskQueue(()->0); var count=new AtomicLong();
        var future=queue.schedule(count::incrementAndGet,0,10,TimeUnit.MILLISECONDS);
        queue.pump(0,(time,runnable)->runnable.run()); queue.pump(0,(time,runnable)->runnable.run()); assertEquals(1,count.get());
        future.cancel(false); queue.pump(20_000_000L,(time,runnable)->runnable.run()); assertEquals(1,count.get());
    }
    @Test void failedGunWorkTerminatesOnlyItsOwnFuture() {
        var queue=new ClockedTaskQueue(()->0); var failed=queue.schedule(()->{throw new IllegalStateException("gun task");},0,1,TimeUnit.MILLISECONDS);
        var count=new AtomicLong(); queue.schedule(count::incrementAndGet,0,1,TimeUnit.MILLISECONDS);
        queue.pump(0,(time,runnable)->runnable.run()); assertTrue(failed.isDone()); assertThrows(ExecutionException.class,failed::get); assertEquals(1,count.get());
    }
    @Test void leavingAWorldCancelsPendingGunWork() {
        var queue=new ClockedTaskQueue(()->0); var task=queue.schedule(()->fail("Stale world task ran"),1,1,TimeUnit.SECONDS);
        queue.clear(); assertTrue(task.isCancelled()); queue.pump(2_000_000_000L,(time,runnable)->runnable.run());
    }
}
