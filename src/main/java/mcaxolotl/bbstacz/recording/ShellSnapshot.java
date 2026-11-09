package mcaxolotl.bbstacz.recording;

/** A shell's emission frame is fixed. Only its age advances; seeking never spawns a second shell. */
public record ShellSnapshot(long id, int origin, float ageMillis, float[] randomVelocity, float[] emissionMatrix, String gunStack) {
    public ShellSnapshot(long id, int origin, float ageMillis, float[] randomVelocity, float[] emissionMatrix) {
        this(id, origin, ageMillis, randomVelocity, emissionMatrix, "");
    }
    public ShellSnapshot {
        if (gunStack == null || gunStack.length() > 1_048_576) throw new IllegalArgumentException("Invalid emission gun NBT");
        if (origin < 0 || origin > 127 || !Float.isFinite(ageMillis)) throw new IllegalArgumentException("Invalid shell");
        PoseMath.requireFinite(randomVelocity, 3);
        PoseMath.requireFinite(emissionMatrix, 16);
        randomVelocity = randomVelocity.clone();
        emissionMatrix = emissionMatrix.clone();
    }
    @Override public float[] randomVelocity() { return randomVelocity.clone(); }
    @Override public float[] emissionMatrix() { return emissionMatrix.clone(); }
    public ShellSnapshot withAge(float age) { return new ShellSnapshot(id, origin, age, randomVelocity, emissionMatrix, gunStack); }
}
