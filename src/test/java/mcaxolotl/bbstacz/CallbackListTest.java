package mcaxolotl.bbstacz;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import mcaxolotl.bbstacz.recording.CallbackList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CallbackListTest {
    @Test void stopIteratorSupportsCallbacksAddingAPlayback() {
        var list = new CallbackList<>(List.of("old"));
        var iterator = list.iterator();
        assertEquals("old", iterator.next()); list.add("new"); iterator.remove();
        assertFalse(iterator.hasNext()); assertEquals(List.of("new"), list);
    }
    @Test void nestedDisconnectDuringTickDoesNotRemoveAReplacement() {
        var list = new CallbackList<>(List.of("old", "other"));
        var seen = new ArrayList<String>();
        list.removeIf(value -> {
            seen.add(value);
            if (value.equals("old")) { list.remove("old"); list.add("old"); }
            return true;
        });
        assertEquals(List.of("old", "other"), seen); assertEquals(List.of("old"), list);
    }
    @Test void newEntriesWaitForTheNextTickAndRemovedEntriesAreSkipped() {
        var list = new CallbackList<>(List.of(1, 2));
        var seen = new ArrayList<Integer>();
        list.removeIf(value -> { seen.add(value); list.remove(Integer.valueOf(2)); list.add(3); return false; });
        assertEquals(List.of(1), seen); assertEquals(List.of(1, 3), list);
    }
    @Test void iteratorRemovalTargetsAnEntryNotAnEqualNewValue() {
        var list = new CallbackList<>(List.of("same", "same"));
        var iterator = list.iterator(); iterator.next(); list.remove(0);
        String replacement = new String("same"); list.add(0, replacement); iterator.remove();
        assertEquals(2, list.size()); assertSame(replacement, list.get(0));
    }
    @Test void concurrentShutdownDuringACallbackIsSafe() throws Exception {
        var list = new CallbackList<>(List.of("old"));
        var entered = new CountDownLatch(1); var replaced = new CountDownLatch(1);
        var task = CompletableFuture.runAsync(() -> list.removeIf(value -> {
            entered.countDown();
            try { assertTrue(replaced.await(3, TimeUnit.SECONDS)); } catch (InterruptedException e) { throw new AssertionError(e); }
            return true;
        }));
        assertTrue(entered.await(3, TimeUnit.SECONDS)); list.clear(); list.add("new"); replaced.countDown();
        task.get(3, TimeUnit.SECONDS); assertEquals(List.of("new"), list);
    }
    @Test void basicListOperationsAndIteratorContractArePreserved() {
        var list = new CallbackList<>(List.of(1, 2)); list.add(1, 3); assertEquals(3, list.set(1, 4));
        assertEquals(List.of(1, 4, 2), list); assertEquals(4, list.remove(1));
        var iterator = list.iterator(); assertThrows(IllegalStateException.class, iterator::remove);
        iterator.next(); iterator.remove(); assertThrows(IllegalStateException.class, iterator::remove);
        assertEquals(List.of(2), list); list.clear(); assertTrue(list.isEmpty());
    }
}
