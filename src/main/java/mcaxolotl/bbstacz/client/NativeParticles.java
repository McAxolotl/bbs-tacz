package mcaxolotl.bbstacz.client;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.ParticleRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource.ParticleDefinitionLoader;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import mcaxolotl.bbstacz.recording.ParticleSnapshot;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class NativeParticles {
    private NativeParticles() {}
    public static void capture(ParticleEmitterInstance emitter, PoseStack pose, float cameraPitch, float cameraRoll, Matrix4f cameraRotation) {
        LiveCapture.Pass pass = LiveCapture.pass();
        if (pass == null || emitter.getParticles().isEmpty() || pass.particles.size() >= 64) return;
        ParticleSnapshot snapshot = snapshot(emitter, pass.inverse, pose.last().pose(), cameraPitch, cameraRoll, cameraRotation);
        if (snapshot != null) pass.particles.add(snapshot);
    }
    public static ParticleSnapshot snapshot(ParticleEmitterInstance emitter, Matrix4f inverse, Matrix4f pose, float cameraPitch, float cameraRoll, Matrix4f cameraRotation) {
        if (emitter.getParticles().isEmpty()) return null;
        var particles = new ArrayList<float[]>();
        for (ParticleInstance p : emitter.getParticles()) { if (particles.size() == 2048) break; particles.add(values(p)); }
        return new ParticleSnapshot(emitter.getDefinition().getIdentifier().toString(),
            new Matrix4f(inverse).mul(pose).get(new float[16]), emitter.getEmitterTransform().get(new float[16]),
            cameraRotation == null ? new Matrix4f().get(new float[16]) : cameraRotation.get(new float[16]), cameraPitch, cameraRoll,
            (emitter.isLocalPosition() ? 1 : 0) | (emitter.isLocalRotation() ? 2 : 0) | (emitter.isLocalVelocity() ? 4 : 0), particles);
    }
    public static float[] values(ParticleInstance p) {
        return new float[]{p.x,p.y,p.z,p.vx,p.vy,p.vz,p.r,p.g,p.b,p.a,p.width,p.height,p.rotation,p.rotationRate,p.age,p.maxLifetime,
            p.random1,p.random2,p.random3,p.random4,p.u0,p.v0,p.u1,p.v1,p.spawnScale,(p.alive?1:0)|(p.worldSpace?2:0)|(p.fpDetached?4:0)|(p.insideKillPlane?8:0)};
    }
    public static ParticleInstance copy(ParticleInstance source) {
        var p = new ParticleInstance(); float[] v = values(source);
        p.x=v[0];p.y=v[1];p.z=v[2];p.vx=v[3];p.vy=v[4];p.vz=v[5];p.r=v[6];p.g=v[7];p.b=v[8];p.a=v[9];p.width=v[10];p.height=v[11];
        p.rotation=v[12];p.rotationRate=v[13];p.age=v[14];p.maxLifetime=v[15];p.random1=v[16];p.random2=v[17];p.random3=v[18];p.random4=v[19];
        p.u0=v[20];p.v0=v[21];p.u1=v[22];p.v1=v[23];p.spawnScale=v[24];p.alive=source.alive;p.worldSpace=source.worldSpace;p.fpDetached=source.fpDetached;p.insideKillPlane=source.insideKillPlane;
        return p;
    }
    public static void render(ParticleSnapshot snapshot, Matrix4f base, MultiBufferSource buffers, int light) {
        var loader = ParticleDefinitionLoader.getInstance();
        var definition = loader.getDefinition(new ResourceLocation(snapshot.definition()));
        if (definition == null) return;
        var emitter = new ParticleEmitterInstance(definition, loader.getMolang());
        emitter.getParticles().clear();
        emitter.setLocalSpaceFlags((snapshot.flags() & 1) != 0, (snapshot.flags() & 2) != 0, (snapshot.flags() & 4) != 0);
        emitter.setEmitterTransform(new Matrix4f().set(snapshot.emitterTransform()), new Matrix4f());
        for (float[] v : snapshot.particles()) {
            ParticleInstance p = new ParticleInstance();
            p.x = v[0]; p.y = v[1]; p.z = v[2]; p.vx = v[3]; p.vy = v[4]; p.vz = v[5];
            p.r = v[6]; p.g = v[7]; p.b = v[8]; p.a = v[9]; p.width = v[10]; p.height = v[11];
            p.rotation = v[12]; p.rotationRate = v[13]; p.age = v[14]; p.maxLifetime = v[15];
            p.random1 = v[16]; p.random2 = v[17]; p.random3 = v[18]; p.random4 = v[19];
            p.u0 = v[20]; p.v0 = v[21]; p.u1 = v[22]; p.v1 = v[23]; p.spawnScale = v[24];
            int flags = (int) v[25]; p.alive = (flags & 1) != 0; p.worldSpace = (flags & 2) != 0;
            p.fpDetached = (flags & 4) != 0; p.insideKillPlane = (flags & 8) != 0; p.emitter = emitter;
            emitter.getParticles().add(p);
        }
        PoseStack pose = new PoseStack();
        Matrix4f matrix = new Matrix4f(base).mul(new Matrix4f().set(snapshot.pose()));
        pose.last().pose().set(matrix);
        pose.last().normal().set(new Matrix3f(matrix).invert().transpose());
        // No tick(), spawnParticle() or animation-clock advance: identical film time -> identical particles.
        ParticleRenderer.render(emitter, pose, buffers, light, 0, snapshot.cameraPitch(), snapshot.cameraRoll(), new Matrix4f().set(snapshot.cameraRotation()));
    }
}
