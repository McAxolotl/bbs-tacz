package mcaxolotl.bbstacz.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mchorse.bbs_mod.utils.VideoRecorder;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.spongepowered.asm.mixin.Mixin;

/** Native BBS readback binds its output texture through raw GL, bypassing Minecraft's binding cache.
 * Do not leave that render-target texture bound as a sampler for the next gun/player draw. */
@Mixin(value=VideoRecorder.class,remap=false)
public abstract class VideoReadbackStateMixin {
    @WrapMethod(method="recordFrame")
    private void bbstacz$restoreReadbackState(Operation<Void> original) {
        if(!((VideoRecorder)(Object)this).isRecording()) { original.call(); return; }
        int unit=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int texture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int pack=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int alignment=GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT);
        try { original.call(); }
        finally {
            GL13.glActiveTexture(unit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pack);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,alignment);
        }
    }
}
