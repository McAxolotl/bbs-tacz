package mcaxolotl.bbstacz.smoke;

/** Test-only GPU-path probe; neither this class nor its mixin ships in the addon. */
public final class MountProbe {
    public static boolean active;
    public static long arms;
    public static boolean neutral = true;
    public static org.joml.Matrix4f matrix;
}
