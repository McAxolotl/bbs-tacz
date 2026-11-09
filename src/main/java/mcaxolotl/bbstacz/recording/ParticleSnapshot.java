package mcaxolotl.bbstacz.recording;

import java.util.ArrayList;
import java.util.List;

/** Evaluated Snowstorm particles. Playback renders these values; it never rerolls Molang randomness. */
public record ParticleSnapshot(String definition, float[] pose, float[] emitterTransform, float[] cameraRotation,
                               float cameraPitch, float cameraRoll, int flags, List<float[]> particles) {
    public static final int PARTICLE_FLOATS = 26;
    public ParticleSnapshot {
        if (definition == null || definition.length() > 1024 || particles.size() > 2048) throw new IllegalArgumentException("Invalid particle snapshot");
        PoseMath.requireFinite(pose, 16); PoseMath.requireFinite(emitterTransform, 16); PoseMath.requireFinite(cameraRotation, 16);
        if (!Float.isFinite(cameraPitch) || !Float.isFinite(cameraRoll)) throw new IllegalArgumentException("Invalid particle camera");
        pose = pose.clone(); emitterTransform = emitterTransform.clone(); cameraRotation = cameraRotation.clone();
        List<float[]> copy = new ArrayList<>();
        for (float[] p : particles) { PoseMath.requireFinite(p, PARTICLE_FLOATS); copy.add(p.clone()); }
        particles = List.copyOf(copy);
    }
    @Override public float[] pose() { return pose.clone(); }
    @Override public float[] emitterTransform() { return emitterTransform.clone(); }
    @Override public float[] cameraRotation() { return cameraRotation.clone(); }
    @Override public List<float[]> particles() { return particles.stream().map(float[]::clone).toList(); }
}
