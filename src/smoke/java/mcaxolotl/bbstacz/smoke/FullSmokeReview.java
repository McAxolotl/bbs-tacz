package mcaxolotl.bbstacz.smoke;

import net.minecraft.client.Minecraft;

/** The same 66-check smoke core, including native mounts, historical shells, crawl and ballistics. */
public final class FullSmokeReview implements ReviewClient.Scenario {
    private final SmokeClient smoke = new SmokeClient();
    @Override public boolean tick(Minecraft client, int tick) {
        if (!smoke.runInWorld(client)) return false;
        ReviewClient.require(smoke.checks() >= 66, "Full smoke lost expected coverage: " + smoke.checks());
        System.out.println("[REVIEW full smoke] PASS " + smoke.checks());
        return true;
    }
}
