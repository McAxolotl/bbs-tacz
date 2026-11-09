package mcaxolotl.bbstacz.smoke;

import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.Files;
import mcaxolotl.bbstacz.client.forms.TaczFormRenderer;
import mcaxolotl.bbstacz.forms.TaczForm;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.resources.Link;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

/** An opaque, unmistakable skin includes the sleeve layers that used to overwrite the selected skin. */
public final class FirstPersonSkinReview implements ReviewClient.Scenario {
    @Override public boolean tick(Minecraft client, int tick) throws Exception {
        if (tick != 10) return false;
        var folder = BBSMod.getAssetsPath("skins").toPath(); Files.createDirectories(folder);
        String file = "review-magenta-" + System.currentTimeMillis() + ".png";
        try (var image = new NativeImage(64,64,false)) {
            for(int y=0;y<64;y++) for(int x=0;x<64;x++) image.setPixelRGBA(x,y,0xffff00ff);
            image.writeToFile(folder.resolve(file));
        }
        TaczForm form = new TaczForm(); var link = new Link("assets", "skins/"+file); form.texture.set(link);
        ReviewClient.require(BBSModClient.getTextures().getTexture(link) != BBSModClient.getTextures().getError(), "Test skin failed to load");
        client.player.getEntityData().set(mchorse.bbs_mod.utils.PlayerUtils.ProtectedAccess.getModelParts(), (byte)127);
        var renderer = (TaczFormRenderer)FormUtilsClient.getRenderer(form);
        int rightUnprimed = NativePreviewSmoke.arm(client, renderer, InteractionHand.MAIN_HAND, "review-skin-right-unprimed.png");
        int leftUnprimed = NativePreviewSmoke.arm(client, renderer, InteractionHand.OFF_HAND, "review-skin-left-unprimed.png");
        int right = NativePreviewSmoke.arm(client, renderer, InteractionHand.MAIN_HAND, "review-skin-right.png", true);
        int left = NativePreviewSmoke.arm(client, renderer, InteractionHand.OFF_HAND, "review-skin-left.png", true);
        System.out.println("[REVIEW skin] selected-skin pixels: right="+right+" left="+left);
        ReviewClient.require(right > 500 && left > 500, "FP sleeve draw overwrote selected skin: right="+right+" left="+left);
        ReviewClient.require(rightUnprimed > 500 && leftUnprimed > 500, "Unprimed FP arm skin regressed");
        var alias = renderer.visualPlayer().getSkinTextureLocation();
        ReviewClient.require(alias.equals(mcaxolotl.bbstacz.client.SkinTextureBridge.skin(new Link("assets", "skins/"+file))), "Equal skins created different aliases");
        int textureId = BBSModClient.getTextures().getTexture(link).id;
        var borrowed = client.getTextureManager().getTexture(alias);
        ReviewClient.require(borrowed.getId() == textureId, "Native texture is not the BBS-selected skin");
        borrowed.close(); borrowed.releaseId();
        ReviewClient.require(org.lwjgl.opengl.GL11.glIsTexture(textureId), "Native alias deleted the BBS-owned skin");
        BBSModClient.getTextures().delete(link);
        textureId = BBSModClient.getTextures().getTexture(link).id;
        ReviewClient.require(borrowed.getId() == textureId, "Native skin alias did not follow a BBS texture reload");
        mcaxolotl.bbstacz.client.SkinTextureBridge.clear();
        ReviewClient.require(org.lwjgl.opengl.GL11.glIsTexture(textureId), "Disconnect cleanup deleted the BBS-owned skin");
        form.texture.set(null);
        renderer.visualPlayer().sync(new mchorse.bbs_mod.forms.entities.MCEntity(client.player), null);
        ReviewClient.require(renderer.visualPlayer().getSkinTextureLocation().equals(client.player.getSkinTextureLocation()), "Default skin fallback retained the previous selected skin");
        return true;
    }
}
