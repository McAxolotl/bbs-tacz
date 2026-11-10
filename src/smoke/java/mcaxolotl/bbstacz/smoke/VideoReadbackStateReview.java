package mcaxolotl.bbstacz.smoke;

import com.mojang.blaze3d.pipeline.TextureTarget;
import java.nio.file.Files;
import java.nio.file.Path;
import mchorse.bbs_mod.utils.VideoRecorder;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/** Native readback must not desynchronize the bound texture from Minecraft's cached GL state. */
public final class VideoReadbackStateReview implements ReviewClient.Scenario {
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick<10) return false;
        var output=Path.of("review-readback-state-"+System.currentTimeMillis()).toAbsolutePath(); Files.createDirectories(output); F4ClockReview.configure(output,30);
        var target=new TextureTarget(32,32,true,Minecraft.ON_OSX); var recorder=new VideoRecorder();
        int before=-1,after=-1;
        try {
            target.setClearColor(0.4f,0.1f,0.3f,1); target.clear(Minecraft.ON_OSX);
            recorder.startRecording("state",null,target.getColorTextureId(),32,32);
            ReviewClient.require(recorder.isRecording(),"Native readback fixture did not start");
            int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,client.getMainRenderTarget().getColorTextureId());
            before=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            recorder.recordFrame(); after=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            recorder.recordFrame();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,previous);
        } finally {
            if(recorder.isRecording()) recorder.stopRecording(false);
            target.destroyBuffers(); client.getMainRenderTarget().bindWrite(true);
        }
        System.out.println("[REVIEW readback binding] before="+before+" after="+after);
        ReviewClient.require(before==after,"Native video readback leaked its capture texture binding into the next render pass"); return true;
    }
}
