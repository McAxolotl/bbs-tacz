package mcaxolotl.bbstacz.recording;

import java.util.Arrays;
import org.joml.Quaternionf;

/** Immutable evaluated native bone: pivot, Euler rotation, animation offset, scale, additional quaternion. */
public final class PartPose {
    private final float[] values;
    public final boolean visible;
    public final boolean illuminated;

    public PartPose(float[] values, boolean visible, boolean illuminated) {
        PoseMath.requireFinite(values, 16);
        this.values = values.clone();
        this.visible = visible;
        this.illuminated = illuminated;
    }
    public float at(int index) { return values[index]; }
    public float[] values() { return values.clone(); }
    public PartPose interpolate(PartPose next, float t) {
        float[] out = values.clone();
        for (int i = 0; i < 12; i++) out[i] = i >= 3 && i < 6
            ? PoseMath.angle(values[i], next.values[i], t) : PoseMath.lerp(values[i], next.values[i], t);
        Quaternionf q = new Quaternionf(values[12], values[13], values[14], values[15]);
        Quaternionf end = new Quaternionf(next.values[12], next.values[13], next.values[14], next.values[15]);
        if (q.lengthSquared() < 1e-8f) q.identity(); else q.normalize();
        if (end.lengthSquared() < 1e-8f) end.identity(); else end.normalize();
        q.slerp(end, t);
        out[12] = q.x; out[13] = q.y; out[14] = q.z; out[15] = q.w;
        return new PartPose(out, visible, illuminated);
    }
    @Override public boolean equals(Object o) {
        return o instanceof PartPose p && visible == p.visible && illuminated == p.illuminated
            && Arrays.equals(values, p.values);
    }
    @Override public int hashCode() { return 31 * Arrays.hashCode(values) + (visible ? 1 : 0) + (illuminated ? 2 : 0); }
}
