package mcaxolotl.bbstacz.recording;

/** A seek restores persistent scene actions, but must not re-fire transient projectiles/sounds backwards. */
public final class ReplayActionScope {
    private static final ThreadLocal<Integer> SEEK=new ThreadLocal<>();
    private ReplayActionScope() {}
    public static boolean seeking() { return SEEK.get()!=null; }
    public static void seek(Runnable actions) {
        Integer previous=SEEK.get(); SEEK.set(previous==null?1:previous+1);
        try { actions.run(); }
        finally { if(previous==null) SEEK.remove(); else SEEK.set(previous); }
    }
}
