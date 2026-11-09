package mcaxolotl.bbstacz;

import java.util.List;
import java.util.Map;
import mcaxolotl.bbstacz.recording.*;
import mchorse.bbs_mod.data.types.MapType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecordingTest {
    static PartPose pose(float angle, boolean visible) {
        return new PartPose(new float[]{0, 0, 0, angle, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 1}, visible, false);
    }
    static FrameSnapshot frame(String gun, long shot, float age, boolean crawl) {
        return new FrameSnapshot(gun, true, Map.of("root", pose(0, true)), Map.of("right_arm", pose(-1.4f, true)),
            PoseMath.identity(), true, crawl, 0.5f, 0.7f, shot, age, 172,
            List.of(new ShellSnapshot(2, 0, age, new float[]{0.1f, 0.2f, 0.3f}, PoseMath.identity())), new float[]{0, 0, 0, 1}, List.of());
    }
    @Test void nativeBendBodyAndItemOffsetsUseTheSameVersionedCodec() {
        var source = new FrameSnapshot("gun", false, Map.of(), Map.of("bend/body", pose(0.3f, true), "item/right", pose(0.7f, true)), PoseMath.identity(), true, false, 0,0,-1,1000,0,List.of(),new float[]{0,0,0,1},List.of());
        assertTrue(FrameFactory.INSTANCE.compare(source, FrameFactory.INSTANCE.fromData(FrameFactory.INSTANCE.toData(source))));
    }
    @Test void shellsRetainTheirEmissionGunAfterSwitching() {
        var shell = new ShellSnapshot(8, 2, 20, new float[]{1,2,3}, PoseMath.identity(), "old_gun");
        var source = new FrameSnapshot("new_gun", true, Map.of(), Map.of(), PoseMath.identity(), true, false, 0,0,1,0,0,List.of(shell),new float[]{0,0,0,1},List.of());
        var copy = FrameFactory.INSTANCE.fromData(FrameFactory.INSTANCE.toData(source));
        assertEquals("old_gun", copy.shells().get(0).gunStack()); assertEquals("old_gun", shell.withAge(50).gunStack());
        var legacy = (MapType) FrameFactory.INSTANCE.toData(source);
        legacy.getList("shells").get(0).asMap().remove("gun");
        assertEquals("", FrameFactory.INSTANCE.fromData(legacy).shells().get(0).gunStack());
    }
    @Test void gunCutsDoNotFreezeContinuingShells() {
        var channel = new mchorse.bbs_mod.utils.keyframes.KeyframeChannel<FrameSnapshot>("state", FrameFactory.INSTANCE);
        channel.insert(0, frame("old", 1, 20, false)); channel.insert(10, frame("new", 1, 520, false));
        var mid = channel.interpolate(5, FrameSnapshot.EMPTY);
        assertEquals("old", mid.gunStack()); assertEquals(270, mid.shells().get(0).ageMillis()); assertEquals(270, mid.shotAgeMillis());
    }
    @Test void holdingTheFinalPoseDoesNotKeepEffectsAliveForever() {
        var channel = new mchorse.bbs_mod.utils.keyframes.KeyframeChannel<FrameSnapshot>("state", FrameFactory.INSTANCE);
        channel.insert(10, frame("gun", 1, 0, false));
        var before = FrameSampler.sample(channel, 0); assertEquals(-1, before.shotId()); assertTrue(before.shells().isEmpty());
        var after = FrameSampler.sample(channel, 20); assertEquals(500, after.shotAgeMillis()); assertEquals(500, after.shells().get(0).ageMillis());
    }
    @Test void nativeDataRoundTripPreservesEverything() {
        var source = frame("{id:\"tacz:modern_kinetic_gun\",Count:1b}", 12, 18.4f, true);
        var encoded = FrameFactory.INSTANCE.toData(source);
        var decoded = FrameFactory.INSTANCE.fromData(encoded);
        assertTrue(FrameFactory.INSTANCE.compare(source, decoded));
        assertEquals(12, decoded.shotId()); assertTrue(decoded.crawling()); assertEquals(172, decoded.flashRotation());
        assertArrayEquals(source.shells().get(0).randomVelocity(), decoded.shells().get(0).randomVelocity());
        assertEquals(FrameFactory.INSTANCE.contentHash(source), FrameFactory.INSTANCE.contentHash(decoded));
    }
    @Test void unsupportedSchemaIsNotSilentlyErased() {
        MapType data = (MapType) FrameFactory.INSTANCE.toData(FrameSnapshot.EMPTY); data.putInt("v", 999);
        assertThrows(IllegalArgumentException.class, () -> FrameFactory.INSTANCE.fromData(data));
    }
    @Test void corruptVectorIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PartPose(new float[]{1}, true, false));
        float[] values = new float[16]; values[3] = Float.NaN;
        assertThrows(IllegalArgumentException.class, () -> new PartPose(values, true, false));
    }
    @Test void rotationsCrossThePiBoundaryByTheShortRoute() {
        PartPose a = pose((float) Math.toRadians(179), true), b = pose((float) Math.toRadians(-179), false);
        PartPose midpoint = a.interpolate(b, 0.5f);
        assertEquals(Math.PI, midpoint.at(3), 1e-5); assertTrue(midpoint.visible);
    }
    @Test void quaternionSignDoesNotCreateAFlip() {
        float[] a = pose(0, true).values(), b = a.clone(); b[15] = -1;
        PartPose midpoint = new PartPose(a, true, false).interpolate(new PartPose(b, true, false), 0.5f);
        assertEquals(1, Math.abs(midpoint.at(15)), 1e-5);
    }
    @Test void matrixInterpolationKeepsAnOrthonormalBasis() {
        float[] a = new Matrix4f().rotateY((float) Math.toRadians(80)).get(new float[16]);
        float[] b = new Matrix4f().translate(4, 2, 6).rotateY((float) Math.toRadians(140)).get(new float[16]);
        Matrix4f matrix = new Matrix4f().set(PoseMath.interpolateMatrix(a, b, 0.5f));
        assertEquals(1, matrix.determinant(), 1e-5);
        assertEquals(new Vector3f(2, 1, 3), matrix.getTranslation(new Vector3f()));
        assertEquals(1, matrix.transformDirection(new Vector3f(1, 0, 0)).length(), 1e-5);
    }
    @Test void collapsedScaleCannotProduceNaN() {
        float[] zero = new Matrix4f().scale(0).get(new float[16]);
        assertArrayEquals(zero, PoseMath.interpolateMatrix(zero, PoseMath.identity(), 0.5f));
    }
    @Test void gunSwitchIsACutNotAPoseMorph() {
        var a = frame("a", 1, 20, false); var b = frame("b", 2, 0, true);
        assertSame(a, a.interpolate(b, 0.99f)); assertSame(b, a.interpolate(b, 1));
    }
    @Test void futureFireCrawlAndHandVisibilityNeverBleedIntoPast() {
        var a = frame("a", 1, 500, false); var b = frame("a", 2, 0, true);
        var mid = a.interpolate(b, 0.8f);
        assertEquals(1, mid.shotId()); assertFalse(mid.crawling()); assertEquals(500, mid.shotAgeMillis());
    }
    @Test void continuousEffectsUseFilmTimeNotSystemTime() {
        var a = frame("a", 1, 10, false); var b = frame("a", 1, 60, false);
        var first = a.interpolate(b, 0.3f); var second = a.interpolate(b, 0.3f);
        assertEquals(25, first.shotAgeMillis()); assertEquals(25, first.shells().get(0).ageMillis());
        assertTrue(FrameFactory.INSTANCE.compare(first, second));
    }
    @Test void snapshotsDefensivelyCopySourceArrays() {
        float[] values = pose(0, true).values(); var source = new PartPose(values, true, false); values[0] = 99;
        float[] exposed = source.values(); exposed[0] = 88; assertEquals(0, source.at(0));
        var shell = new ShellSnapshot(1, 0, 10, new float[]{1, 2, 3}, PoseMath.identity());
        shell.randomVelocity()[0] = 100; assertEquals(1, shell.randomVelocity()[0]);
    }
    @Test void recordedLightAndAdsFovRoundTrip() {
        var source = new FrameSnapshot("gun", true, Map.of(), Map.of(), PoseMath.identity(), true, false, 1, 0, 1, 20, 10,
            List.of(), new float[]{0, 0, 0, 1}, List.of(), 15728880, 35, 45);
        var restored = FrameFactory.INSTANCE.fromData(FrameFactory.INSTANCE.toData(source));
        assertEquals(15728880, restored.modelLight()); assertEquals(35, restored.worldFov()); assertEquals(45, restored.modelFov());
    }
    @Test void emptyFrameAlsoRoundTrips() { assertTrue(FrameFactory.INSTANCE.compare(FrameSnapshot.EMPTY, FrameFactory.INSTANCE.fromData(FrameFactory.INSTANCE.toData(FrameSnapshot.EMPTY)))); }
    @Test void snowstormRandomValuesAndUvAreSerialized() {
        float[] particle = new float[ParticleSnapshot.PARTICLE_FLOATS]; particle[16] = 0.234f; particle[22] = 0.75f; particle[25] = 7;
        var p = new ParticleSnapshot("tacz:muzzle_flash", PoseMath.identity(), PoseMath.identity(), PoseMath.identity(), 20, 15, 3, List.of(particle));
        var source = new FrameSnapshot("gun", true, Map.of(), Map.of(), PoseMath.identity(), true, false, 0, 0, 1, 1, 45,
            List.of(), new float[]{0, 0, 0, 1}, List.of(p));
        var restored = FrameFactory.INSTANCE.fromData(FrameFactory.INSTANCE.toData(source));
        assertArrayEquals(particle, restored.particles().get(0).particles().get(0));
        assertEquals(3, restored.particles().get(0).flags());
    }
}
