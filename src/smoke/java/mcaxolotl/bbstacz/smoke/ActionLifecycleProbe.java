package mcaxolotl.bbstacz.smoke;

/** Deterministic simulation of a disconnect/stop callback while native ActionManager iterates. */
public final class ActionLifecycleProbe {
    public static String film;
    public static Runnable tick, stop;
    private ActionLifecycleProbe() {}
}
