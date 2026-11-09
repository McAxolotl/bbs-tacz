package mcaxolotl.bbstacz.client;

/** Start the timeline only after the first actual video texture readback, not merely after starting FFmpeg. */
public final class F6FrameGate {
    private static Object owner;
    private static Runnable pending;
    private F6FrameGate() {}
    public static void await(Object ticket, Runnable callback) { owner=ticket; pending=callback; }
    public static void captured() { Runnable callback=pending; pending=null; owner=null; if(callback!=null) callback.run(); }
    public static boolean waiting(Object ticket) { return owner==ticket && pending!=null; }
    public static void cancel(Object ticket) { if(owner==ticket) clear(); }
    public static void clear() { pending=null; owner=null; }
}
