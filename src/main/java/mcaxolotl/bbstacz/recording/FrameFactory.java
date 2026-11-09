package mcaxolotl.bbstacz.recording;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;

/** Versioned native BBS data: usable by both the client and dedicated server, no Java object serialization. */
public final class FrameFactory implements IKeyframeFactory<FrameSnapshot> {
    public static final FrameFactory INSTANCE = new FrameFactory();
    public static final int SCHEMA = 1;
    private final Map<FrameSnapshot, Integer> hashes = new WeakHashMap<>();
    private FrameFactory() {}
    @Override public FrameSnapshot createEmpty() { return FrameSnapshot.EMPTY; }
    @Override public FrameSnapshot copy(FrameSnapshot frame) { return frame; } // All members are immutable/defensively copied.
    @Override public boolean compare(Object a, Object b) {
        return a == b || a instanceof FrameSnapshot fa && b instanceof FrameSnapshot fb
            && contentHash(fa) == contentHash(fb) && toData(fa).equals(toData(fb));
    }
    @Override public synchronized int contentHash(FrameSnapshot frame) {
        return hashes.computeIfAbsent(frame, f -> stableHash(toData(f)));
    }
    private static int stableHash(BaseType data) {
        if (data instanceof MapType map) {
            int hash = 0;
            for (String key : map.keys()) hash += key.hashCode() ^ stableHash(map.get(key));
            return hash;
        }
        if (data instanceof ListType list) { int hash = 1; for (BaseType element : list) hash = 31 * hash + stableHash(element); return hash; }
        return 31 * data.getTypeId() + data.toString().hashCode();
    }
    @Override public FrameSnapshot interpolate(Keyframe<FrameSnapshot> pre, Keyframe<FrameSnapshot> a, Keyframe<FrameSnapshot> b,
                                                Keyframe<FrameSnapshot> post, IInterp interpolation, float t) {
        t = Math.max(0, Math.min(1, t));
        FrameSnapshot out = a.getValue().interpolate(b.getValue(), t);
        if (t == 0 || t == 1) return out;
        float elapsed = (b.getTick() - a.getTick()) * t * 50;
        float age = a.getValue().shotId() == b.getValue().shotId() ? PoseMath.lerp(a.getValue().shotAgeMillis(), b.getValue().shotAgeMillis(), t) : a.getValue().shotAgeMillis() + elapsed;
        var shells = new ArrayList<ShellSnapshot>();
        for (var shell : out.shells()) {
            var next = b.getValue().shells().stream().filter(s -> s.id() == shell.id() && s.origin() == shell.origin()).findFirst().orElse(null);
            // Lifetime remains continuous even if the held item changes and gun pose interpolation cuts.
            shells.add(shell.withAge(next == null ? shell.ageMillis() + elapsed : PoseMath.lerp(a.getValue().shells().stream()
                .filter(s -> s.id() == shell.id() && s.origin() == shell.origin()).findFirst().orElse(shell).ageMillis(), next.ageMillis(), t)));
        }
        return new FrameSnapshot(out.gunStack(), out.firstPerson(), out.gunBones(), out.humanoid(), out.gunMatrix(), out.renderHands(),
            out.crawling(), out.aiming(), out.refit(), out.shotId(), age, out.flashRotation(), shells, out.handCamera(), out.particles(), out.modelLight(), out.worldFov(), out.modelFov());
    }
    @Override public FrameSnapshot interpolate(FrameSnapshot pre, FrameSnapshot a, FrameSnapshot b, FrameSnapshot post, IInterp interpolation, float t) {
        // No cubic overshoot for transforms, effect lifetimes or aiming progress.
        return a.interpolate(b, Math.max(0, Math.min(1, t)));
    }
    @Override public BaseType toData(FrameSnapshot frame) {
        MapType map = new MapType();
        map.putInt("v", SCHEMA); map.putString("gun", frame.gunStack()); map.putBool("fp", frame.firstPerson());
        map.put("bones", bones(frame.gunBones())); map.put("body", bones(frame.humanoid())); map.put("matrix", numbers(frame.gunMatrix()));
        map.putBool("hands", frame.renderHands()); map.putBool("crawl", frame.crawling());
        map.putFloat("aim", frame.aiming()); map.putFloat("refit", frame.refit());
        map.putLong("shot", frame.shotId()); map.putFloat("shot_age", frame.shotAgeMillis()); map.putFloat("flash_roll", frame.flashRotation());
        ListType shells = new ListType();
        for (ShellSnapshot shell : frame.shells()) {
            MapType s = new MapType(); s.putLong("id", shell.id()); s.putInt("origin", shell.origin()); s.putFloat("age", shell.ageMillis());
            s.put("random", numbers(shell.randomVelocity())); s.put("matrix", numbers(shell.emissionMatrix())); if (!shell.gunStack().isEmpty()) s.putString("gun", shell.gunStack()); shells.add(s);
        }
        map.put("shells", shells); map.put("hand_camera", numbers(frame.handCamera()));
        ListType particles = new ListType();
        for (ParticleSnapshot p : frame.particles()) {
            MapType emitter = new MapType(); emitter.putString("definition", p.definition());
            emitter.put("pose", numbers(p.pose())); emitter.put("transform", numbers(p.emitterTransform()));
            emitter.put("camera", numbers(p.cameraRotation())); emitter.putFloat("pitch", p.cameraPitch());
            emitter.putFloat("roll", p.cameraRoll()); emitter.putInt("flags", p.flags());
            ListType instances = new ListType(); for (float[] particle : p.particles()) instances.add(numbers(particle));
            emitter.put("particles", instances); particles.add(emitter);
        }
        map.put("particles", particles); map.putInt("light", frame.modelLight());
        map.putFloat("world_fov", frame.worldFov()); map.putFloat("model_fov", frame.modelFov());
        return map;
    }
    @Override public FrameSnapshot fromData(BaseType data) {
        if (!(data instanceof MapType map)) throw new IllegalArgumentException("BBS TACZ frame must be a map");
        if (map.getInt("v") != SCHEMA) throw new IllegalArgumentException("Unsupported BBS TACZ recording schema " + map.getInt("v"));
        var shells = new ArrayList<ShellSnapshot>();
        ListType list = map.getList("shells");
        if (list.size() > 128) throw new IllegalArgumentException("Too many shells");
        for (BaseType element : list) {
            MapType shell = element.asMap();
            shells.add(new ShellSnapshot(shell.getLong("id"), shell.getInt("origin"), shell.getFloat("age"),
                floats(shell.getList("random"), 3), floats(shell.getList("matrix"), 16), shell.getString("gun", "")));
        }
        var particles = new ArrayList<ParticleSnapshot>();
        ListType emitters = map.getList("particles");
        if (emitters.size() > 64) throw new IllegalArgumentException("Too many particle emitters");
        for (BaseType element : emitters) {
            MapType p = element.asMap(); var instances = new ArrayList<float[]>();
            if (p.getList("particles").size() > 2048) throw new IllegalArgumentException("Too many particles");
            for (BaseType instance : p.getList("particles")) instances.add(floats(instance.asList(), ParticleSnapshot.PARTICLE_FLOATS));
            particles.add(new ParticleSnapshot(p.getString("definition"), floats(p.getList("pose"), 16),
                floats(p.getList("transform"), 16), floats(p.getList("camera"), 16), p.getFloat("pitch"), p.getFloat("roll"), p.getInt("flags"), instances));
        }
        return new FrameSnapshot(map.getString("gun"), map.getBool("fp"), readBones(map.getMap("bones"), 2048),
            readBones(map.getMap("body"), 16), floats(map.getList("matrix"), 16), map.getBool("hands", true),
            map.getBool("crawl"), map.getFloat("aim"), map.getFloat("refit"), map.getLong("shot", -1),
            map.getFloat("shot_age", 1000), map.getFloat("flash_roll"), shells,
            map.has("hand_camera") ? floats(map.getList("hand_camera"), 4) : new float[]{0, 0, 0, 1}, particles, map.getInt("light", -1), map.getFloat("world_fov", -1), map.getFloat("model_fov", -1));
    }
    private static MapType bones(Map<String, PartPose> poses) {
        MapType out = new MapType();
        poses.forEach((name, pose) -> { ListType p = numbers(pose.values()); p.addBool(pose.visible); p.addBool(pose.illuminated); out.put(name, p); });
        return out;
    }
    private static Map<String, PartPose> readBones(MapType map, int limit) {
        if (map.keys().size() > limit) throw new IllegalArgumentException("Too many bones");
        Map<String, PartPose> out = new LinkedHashMap<>();
        for (String key : map.keys()) {
            ListType p = map.getList(key);
            if (p.size() != 18) throw new IllegalArgumentException("Invalid bone pose " + key);
            float[] numbers = new float[16]; for (int i = 0; i < 16; i++) numbers[i] = p.getFloat(i);
            out.put(key, new PartPose(numbers, p.getBool(16), p.getBool(17)));
        }
        return out;
    }
    public static ListType numbers(float[] values) { ListType out = new ListType(); for (float n : values) out.addFloat(n); return out; }
    public static float[] floats(ListType list, int size) {
        if (list.size() != size) throw new IllegalArgumentException("Invalid matrix/vector length");
        float[] out = new float[size]; for (int i = 0; i < size; i++) out[i] = list.getFloat(i);
        PoseMath.requireFinite(out, size); return out;
    }
}
