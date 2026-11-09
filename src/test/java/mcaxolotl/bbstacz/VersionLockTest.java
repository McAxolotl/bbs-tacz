package mcaxolotl.bbstacz;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VersionLockTest {
    @Test void projectAndManifestRemainAtUserRequestedVersion() throws Exception {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("gradle.properties"))) { properties.load(reader); }
        assertEquals("v1.0.0", properties.getProperty("mod_version"));
        var manifest = JsonParser.parseString(Files.readString(Path.of("src/main/resources/fabric.mod.json"))).getAsJsonObject();
        assertEquals("v1.0.0", manifest.get("version").getAsString());
    }
}
