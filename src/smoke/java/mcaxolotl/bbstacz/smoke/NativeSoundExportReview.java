package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.client.sound.SoundPlayManager;
import java.nio.file.Files;
import java.nio.file.Path;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.audio.MinecraftSoundMixer;
import mchorse.bbs_mod.audio.wav.WaveReader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** The native Minecraft-sound export path must resolve TACZ's actual tacz_sounds OGG, not a sound-event id. */
public final class NativeSoundExportReview implements ReviewClient.Scenario {
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        var capture=BBSModClient.getMinecraftSoundCapture();
        if(tick==0) capture.begin();
        capture.captureFrame();
        if(tick==10) ReviewClient.require(SoundPlayManager.playClientSound(client.player,new ResourceLocation("tacz","ak47/ak47_shoot"),0.8f,0.95f,30)!=null,"Native TACZ sound failed to resolve");
        if(tick==35) {
            capture.end();
            var file=Path.of("review-native-tacz-audio.wav");
            ReviewClient.require(!capture.getSounds().isEmpty(),"The native video sound capture recorded no sound events");
            ReviewClient.require(MinecraftSoundMixer.mixToFile(file.toFile(),capture.getSounds(),capture.getFrames(),null,48000,20d,capture.getFrames().size()),"Native sound mixing failed");
            mchorse.bbs_mod.audio.Wave wave; try(var input=Files.newInputStream(file)) { wave=new WaveReader().read(input); } long nonzero=0; for(byte b:wave.data) if(b!=0) nonzero++;
            System.out.println("[REVIEW sound export] paths="+capture.getSounds().stream().map(s->s.location).toList()+" nonzero PCM bytes="+nonzero);
            ReviewClient.require(nonzero>1000,"Native TACZ sound export is silent despite recorded sound events");
            return true;
        }
        return false;
    }
}
