package mcaxolotl.bbstacz.client;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.resources.Link;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Native render layers bind a BBS skin by identity, never by the order in which buffers flush. */
public final class SkinTextureBridge {
    private static final Map<Link, ResourceLocation> ALIASES = new HashMap<>();
    private SkinTextureBridge() {}

    public static ResourceLocation skin(Link link) {
        if (link == null) return null;
        return ALIASES.computeIfAbsent(link, key -> {
            var id = new ResourceLocation("bbs_tacz", "skins/" + UUID.nameUUIDFromBytes(key.toString().getBytes(StandardCharsets.UTF_8)));
            Minecraft.getInstance().getTextureManager().register(id, new BorrowedTexture(key));
            return id;
        });
    }

    public static void clear() {
        var textures = Minecraft.getInstance().getTextureManager();
        ALIASES.values().forEach(textures::release);
        ALIASES.clear();
    }

    private static final class BorrowedTexture extends AbstractTexture {
        private final Link link;
        private BorrowedTexture(Link link) { this.link = link; }
        // Resolve on each bind: BBS owns hot reloads, animated frames and the GL object's lifetime.
        @Override public int getId() { return BBSModClient.getTextures().getTexture(link).id; }
        @Override public void load(ResourceManager resources) {}
        @Override public void releaseId() {}
        @Override public void close() {}
    }
}
