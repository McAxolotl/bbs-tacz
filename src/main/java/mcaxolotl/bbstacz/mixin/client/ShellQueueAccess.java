package mcaxolotl.bbstacz.mixin.client;

import com.tacz.guns.client.model.functional.ShellRender;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ShellRender.class, remap = false)
public interface ShellQueueAccess {
    @Accessor("SHELL_QUEUE") ConcurrentLinkedDeque<ShellRender.Data> bbstacz$shells();
}
