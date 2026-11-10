package mcaxolotl.bbstacz.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.FilmManager;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.manager.BaseManager;
import org.spongepowered.asm.mixin.Mixin;

/** BBS repository SAVE can run on its packet worker while a server START reads the same gzip.
 * Serialize complete native film reads/writes (including backups), without replacing storage or permissions. */
@Mixin(value=BaseManager.class,remap=false)
public abstract class FilmStorageMixin {
    @WrapMethod(method="load(Ljava/lang/String;)Lmchorse/bbs_mod/settings/values/core/ValueGroup;")
    private ValueGroup bbstacz$readFilm(String id,Operation<ValueGroup> original) {
        if(!((Object)this instanceof FilmManager)) return original.call(id);
        synchronized(this) { return original.call(id); }
    }
    @WrapMethod(method="save")
    private boolean bbstacz$writeFilm(String id,MapType data,Operation<Boolean> original) {
        if(!((Object)this instanceof FilmManager)) return original.call(id,data);
        synchronized(this) { return original.call(id,data); }
    }
}
