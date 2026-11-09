package mcaxolotl.bbstacz.recording;

import java.util.List;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

/** BBS holds the last key outside a channel. Poses may hold, but flashes and shell lifetimes must not freeze there. */
public final class FrameSampler {
    private FrameSampler() {}
    public static FrameSnapshot sample(KeyframeChannel<FrameSnapshot> channel, float tick) {
        if (channel.isEmpty()) return FrameSnapshot.EMPTY;
        var keys = channel.getKeyframes(); var first = keys.get(0);
        var frame = channel.interpolate(tick, FrameSnapshot.EMPTY);
        if (tick < first.getTick()) return withEffects(frame, -1, 1000, List.of(), List.of());
        var last = keys.get(keys.size() - 1);
        if (tick <= last.getTick()) return frame;
        float elapsed = (tick - last.getTick()) * 50f;
        // No new particle simulation beyond the final baked frame. Expire that final evaluated batch rather than leaking it forever.
        var particles = new java.util.ArrayList<ParticleSnapshot>();
        for (var emitter : frame.particles()) {
            var values = new java.util.ArrayList<float[]>();
            for (float[] p : emitter.particles()) {
                float age = p[14] + elapsed / 1000f;
                if (age >= p[15] || (((int)p[25]) & 1) == 0) continue;
                p[0] += p[3] * elapsed / 1000f; p[1] += p[4] * elapsed / 1000f; p[2] += p[5] * elapsed / 1000f; p[14] = age;
                values.add(p);
            }
            if (!values.isEmpty()) particles.add(new ParticleSnapshot(emitter.definition(), emitter.pose(), emitter.emitterTransform(), emitter.cameraRotation(), emitter.cameraPitch(), emitter.cameraRoll(), emitter.flags(), values));
        }
        return withEffects(frame, frame.shotId(), frame.shotAgeMillis() + elapsed,
            frame.shells().stream().map(s -> s.withAge(s.ageMillis() + elapsed)).toList(), particles);
    }
    private static FrameSnapshot withEffects(FrameSnapshot f, long shot, float age, List<ShellSnapshot> shells, List<ParticleSnapshot> particles) {
        return new FrameSnapshot(f.gunStack(), f.firstPerson(), f.gunBones(), f.humanoid(), f.gunMatrix(), f.renderHands(), f.crawling(), f.aiming(), f.refit(),
            shot, age, f.flashRotation(), shells, f.handCamera(), particles, f.modelLight(), f.worldFov(), f.modelFov());
    }
}
