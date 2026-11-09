package mcaxolotl.bbstacz.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import mcaxolotl.bbstacz.BBSTaczAddon;
import net.fabricmc.loader.api.FabricLoader;

/** Runtime capture settings; form appearance lives in the dedicated form, not model-specific rig profiles. */
public final class TaczConfig {
    public static TaczConfig INSTANCE = new TaczConfig();
    public boolean firstPersonReplay = true;
    public int recordingFps = 60;
    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("bbs_tacz.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.isRegularFile(file)) { var config = gson.fromJson(Files.readString(file), TaczConfig.class); if (config != null) INSTANCE = config; }
            else { Files.createDirectories(file.getParent()); Files.writeString(file, gson.toJson(INSTANCE)); }
            INSTANCE.recordingFps = Math.max(20, Math.min(240, INSTANCE.recordingFps));
        } catch (IOException | RuntimeException e) { BBSTaczAddon.LOGGER.warn("Cannot load {}; using defaults", file, e); INSTANCE = new TaczConfig(); }
    }
}
