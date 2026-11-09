package mcaxolotl.bbstacz.recording;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Math shared by recording and playback. No game clock is consulted. */
public final class PoseMath {
    private PoseMath() {}

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    public static float angle(float a, float b, float t) {
        float delta = (float) Math.IEEEremainder(b - a, Math.PI * 2);
        return a + delta * t;
    }
    public static float[] identity() { return new Matrix4f().get(new float[16]); }

    public static float[] interpolateMatrix(float[] a, float[] b, float t) {
        Matrix4f ma = new Matrix4f().set(a), mb = new Matrix4f().set(b);
        Vector3f pa = ma.getTranslation(new Vector3f()), pb = mb.getTranslation(new Vector3f());
        Vector3f sa = ma.getScale(new Vector3f()), sb = mb.getScale(new Vector3f());
        // A collapsed/singular authored scale cannot be decomposed safely.
        if (Math.abs(ma.determinant()) < 1e-8f || Math.abs(mb.determinant()) < 1e-8f) return a.clone();
        if (ma.determinant() < 0) sa.x = -sa.x;
        if (mb.determinant() < 0) sb.x = -sb.x;
        Quaternionf qa = new Matrix4f(ma).scale(1 / sa.x, 1 / sa.y, 1 / sa.z)
            .getNormalizedRotation(new Quaternionf()).normalize();
        Quaternionf qb = new Matrix4f(mb).scale(1 / sb.x, 1 / sb.y, 1 / sb.z)
            .getNormalizedRotation(new Quaternionf()).normalize();
        return new Matrix4f().translationRotateScale(pa.lerp(pb, t), qa.slerp(qb, t), sa.lerp(sb, t))
            .get(new float[16]);
    }

    public static void requireFinite(float[] numbers, int length) {
        if (numbers.length != length) throw new IllegalArgumentException("Expected " + length + " pose components");
        for (float number : numbers) if (!Float.isFinite(number)) throw new IllegalArgumentException("Non-finite pose");
    }

}
