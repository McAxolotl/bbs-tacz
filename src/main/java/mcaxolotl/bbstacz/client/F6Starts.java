package mcaxolotl.bbstacz.client;

import java.util.ArrayDeque;
import java.util.HashMap;

/** Ordered native play responses have no request id; keep per-film tickets so cancelled replies cannot resurrect a film. */
public final class F6Starts {
    public static final class Ticket {
        private final long expires = System.currentTimeMillis() + 60_000;
        private boolean cancelled;
        private Ticket() {}
    }
    private static final HashMap<String, Ticket> PREPARED = new HashMap<>();
    private static final HashMap<String, ArrayDeque<Ticket>> REQUESTS = new HashMap<>();
    private F6Starts() {}
    private static void reap() {
        long now=System.currentTimeMillis();
        PREPARED.values().removeIf(ticket -> ticket.expires < now || ticket.cancelled);
        REQUESTS.values().removeIf(queue -> { queue.removeIf(ticket -> ticket.expires < now); return queue.isEmpty(); });
    }
    public static synchronized Ticket request(String film) {
        reap(); var ticket=new Ticket(); REQUESTS.computeIfAbsent(film,key->new ArrayDeque<>()).addLast(ticket); return ticket;
    }
    public static synchronized void cancel(Ticket ticket) { if(ticket!=null) ticket.cancelled=true; }
    /** True means only this cancelled reply should be dropped; a newer retry retains its own ticket. */
    public static synchronized boolean receive(String film) {
        reap(); var queue=REQUESTS.get(film); if(queue==null) return false;
        var ticket=queue.removeFirst(); if(queue.isEmpty()) REQUESTS.remove(film);
        if(!ticket.cancelled) PREPARED.put(film,ticket); return ticket.cancelled;
    }
    public static synchronized boolean holdInitialFrame(String film) { Ticket ticket=PREPARED.remove(film); return ticket!=null&&!ticket.cancelled; }
    public static synchronized void clear() { REQUESTS.clear(); PREPARED.clear(); }
}
