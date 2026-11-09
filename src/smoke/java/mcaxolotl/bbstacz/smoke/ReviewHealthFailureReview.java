package mcaxolotl.bbstacz.smoke;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

/** Intentional negative self-test: teardown/Gradle must reject PASS when a fresh crash marker exists. */
public final class ReviewHealthFailureReview implements ReviewClient.Scenario {
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        Path marker=Path.of("crash-reports","bbs-tacz-validation-marker-"+System.currentTimeMillis()+".txt");
        Files.createDirectories(marker.getParent()); Files.writeString(marker,"INJECTED VALIDATION MARKER; not a real Minecraft crash.\n");
        System.out.println("[REVIEW health negative control] created "+marker);
        return true;
    }
}
