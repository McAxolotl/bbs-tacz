package mcaxolotl.bbstacz.recording;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A film-time snapshot, not a wall-clock timestamp or a reference to TACZ's shared display. */
public record FrameSnapshot(String gunStack, boolean firstPerson, Map<String, PartPose> gunBones,
                            Map<String, PartPose> humanoid, float[] gunMatrix, boolean renderHands,
                            boolean crawling, float aiming, float refit, long shotId,
                            float shotAgeMillis, float flashRotation, List<ShellSnapshot> shells,
                            float[] handCamera, List<ParticleSnapshot> particles, int modelLight, float worldFov, float modelFov) {
    public static final FrameSnapshot EMPTY = new FrameSnapshot("", false, Map.of(), Map.of(),
        PoseMath.identity(), true, false, 0, 0, -1, 1000, 0, List.of(), new float[]{0, 0, 0, 1}, List.of());
    public FrameSnapshot(String gunStack, boolean firstPerson, Map<String, PartPose> gunBones, Map<String, PartPose> humanoid,
                         float[] gunMatrix, boolean renderHands, boolean crawling, float aiming, float refit, long shotId,
                         float shotAgeMillis, float flashRotation, List<ShellSnapshot> shells, float[] handCamera, List<ParticleSnapshot> particles) {
        this(gunStack, firstPerson, gunBones, humanoid, gunMatrix, renderHands, crawling, aiming, refit, shotId, shotAgeMillis,
            flashRotation, shells, handCamera, particles, -1, -1, -1);
    }
    public FrameSnapshot {
        if (gunStack == null || gunStack.length() > 1_048_576) throw new IllegalArgumentException("Invalid gun NBT");
        if (gunBones.size() > 2048 || humanoid.size() > 16 || shells.size() > 128) throw new IllegalArgumentException("Oversized pose");
        PoseMath.requireFinite(gunMatrix, 16);
        PoseMath.requireFinite(handCamera, 4);
        if (particles.size() > 64) throw new IllegalArgumentException("Too many emitters");
        if (!Float.isFinite(aiming) || !Float.isFinite(refit) || !Float.isFinite(shotAgeMillis) || !Float.isFinite(flashRotation) || !Float.isFinite(worldFov) || !Float.isFinite(modelFov))
            throw new IllegalArgumentException("Invalid state");
        gunBones = Map.copyOf(gunBones);
        humanoid = Map.copyOf(humanoid);
        gunMatrix = gunMatrix.clone();
        shells = List.copyOf(shells); handCamera = handCamera.clone(); particles = List.copyOf(particles);
    }
    @Override public float[] gunMatrix() { return gunMatrix.clone(); }
    @Override public float[] handCamera() { return handCamera.clone(); }

    public FrameSnapshot interpolate(FrameSnapshot next, float t) {
        if (t <= 0) return this;
        if (t >= 1) return next;
        // An item switch is a cut, not a morph between unrelated bone hierarchies.
        if (!gunStack.equals(next.gunStack) || firstPerson != next.firstPerson) return this;
        List<ShellSnapshot> interpolatedShells = new ArrayList<>();
        for (ShellSnapshot shell : shells) {
            ShellSnapshot matching = next.shells.stream().filter(s -> s.id() == shell.id() && s.origin() == shell.origin()).findFirst().orElse(null);
            interpolatedShells.add(matching == null ? shell : shell.withAge(PoseMath.lerp(shell.ageMillis(), matching.ageMillis(), t)));
        }
        // Discrete events stay on the preceding key. In particular a future shot must not flash early.
        float age = shotId == next.shotId ? PoseMath.lerp(shotAgeMillis, next.shotAgeMillis, t) : shotAgeMillis;
        return new FrameSnapshot(gunStack, firstPerson, blend(gunBones, next.gunBones, t), blend(humanoid, next.humanoid, t),
            PoseMath.interpolateMatrix(gunMatrix, next.gunMatrix, t), renderHands, crawling,
            PoseMath.lerp(aiming, next.aiming, t), PoseMath.lerp(refit, next.refit, t), shotId,
            age, flashRotation, interpolatedShells, blendQuaternion(handCamera, next.handCamera, t), particles, modelLight,
            PoseMath.lerp(worldFov, next.worldFov, t), PoseMath.lerp(modelFov, next.modelFov, t));
    }
    private static float[] blendQuaternion(float[] a, float[] b, float t) {
        var q = new org.joml.Quaternionf(a[0], a[1], a[2], a[3]);
        var end = new org.joml.Quaternionf(b[0], b[1], b[2], b[3]);
        if (q.lengthSquared() < 1e-8f) q.identity(); else q.normalize();
        if (end.lengthSquared() < 1e-8f) end.identity(); else end.normalize();
        q.slerp(end, t); return new float[]{q.x, q.y, q.z, q.w};
    }
    private static Map<String, PartPose> blend(Map<String, PartPose> a, Map<String, PartPose> b, float t) {
        Map<String, PartPose> out = new LinkedHashMap<>();
        a.forEach((name, pose) -> out.put(name, b.containsKey(name) ? pose.interpolate(b.get(name), t) : pose));
        return out;
    }
}
