package mcaxolotl.bbstacz.recording;

import java.util.PriorityQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

/** Fixed-rate native gun work on animation time. No background real-time scheduler races a captured frame. */
public final class ClockedTaskQueue {
    private final LongSupplier clock;
    private final PriorityQueue<Task> tasks=new PriorityQueue<>();
    private long sequence;
    public ClockedTaskQueue(LongSupplier clock) { this.clock=clock; }
    public synchronized ScheduledFuture<?> schedule(Runnable runnable,long delay,long period,TimeUnit unit) {
        if(period<=0) throw new IllegalArgumentException("Nonpositive gun task period");
        var task=new Task(runnable,clock.getAsLong()+Math.max(0,unit.toNanos(delay)),unit.toNanos(period),++sequence);
        tasks.add(task); return task;
    }
    public void pump(long now,BiConsumer<Long,Runnable> runAtTime) {
        for(int count=0;count<10_000;count++) {
            Task task;
            synchronized(this) {
                task=tasks.peek(); if(task==null || task.due>now) return;
                tasks.remove(); if(task.done) continue;
            }
            try { runAtTime.accept(task.due,task.runnable); }
            catch(Throwable failure) { task.fail(failure); }
            synchronized(this) { if(!task.done) { task.due+=task.period; tasks.add(task); } }
        }
    }
    public synchronized void clear() { tasks.forEach(task->task.cancel(false)); tasks.clear(); }
    private final class Task implements ScheduledFuture<Object> {
        private final Runnable runnable;
        private final long period,id;
        private volatile long due;
        private volatile boolean done,cancelled;
        private Throwable failure;
        Task(Runnable runnable,long due,long period,long id) { this.runnable=runnable; this.due=due; this.period=period; this.id=id; }
        synchronized void fail(Throwable failure) { this.failure=failure; done=true; notifyAll(); }
        @Override public synchronized boolean cancel(boolean interrupt) { if(done) return false; cancelled=done=true; notifyAll(); return true; }
        @Override public boolean isCancelled() { return cancelled; }
        @Override public boolean isDone() { return done; }
        @Override public long getDelay(TimeUnit unit) { return unit.convert(due-clock.getAsLong(),TimeUnit.NANOSECONDS); }
        @Override public int compareTo(Delayed other) {
            if(other instanceof ClockedTaskQueue.Task task) { int order=Long.compare(due,task.due); return order==0?Long.compare(id,task.id):order; }
            return Long.compare(getDelay(TimeUnit.NANOSECONDS),other.getDelay(TimeUnit.NANOSECONDS));
        }
        @Override public synchronized Object get() throws InterruptedException,ExecutionException { while(!done) wait(); return result(); }
        @Override public synchronized Object get(long timeout,TimeUnit unit) throws InterruptedException,ExecutionException,TimeoutException {
            long remaining=unit.toNanos(timeout),end=System.nanoTime()+remaining;
            while(!done) { if(remaining<=0) throw new TimeoutException(); TimeUnit.NANOSECONDS.timedWait(this,remaining); remaining=end-System.nanoTime(); }
            return result();
        }
        private Object result() throws ExecutionException { if(cancelled) throw new CancellationException(); if(failure!=null) throw new ExecutionException(failure); return null; }
    }
}
