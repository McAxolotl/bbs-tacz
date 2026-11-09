package mcaxolotl.bbstacz;

import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IconResourceTest {
    @Test void manifestPackagesTheExactUserProvidedIcon() throws Exception {
        var metadata=JsonParser.parseString(Files.readString(Path.of("src/main/resources/fabric.mod.json"))).getAsJsonObject();
        String icon=metadata.get("icon").getAsString(); assertEquals("assets/bbs_tacz/icon.png",icon);
        try(var input=IconResourceTest.class.getClassLoader().getResourceAsStream(icon)) {
            assertNotNull(input,"Manifest icon was not packaged"); var data=input.readAllBytes();
            assertEquals("d8d28b0a8a2ef472cec082097fb612a79dfecece665cb8e6e41ba9e97287dd6d",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)));
            var image=ImageIO.read(new ByteArrayInputStream(data)); assertNotNull(image); assertEquals(256,image.getWidth()); assertEquals(256,image.getHeight());
        }
        assertEquals("v1.0.0",metadata.get("version").getAsString());
    }
}
