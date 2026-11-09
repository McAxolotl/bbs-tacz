package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.client.sound.GunSoundInstance;
import com.tacz.guns.client.sound.SoundPlayManager;
import java.nio.file.Files;
import java.nio.file.Path;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.audio.MinecraftSoundMixer;
import mchorse.bbs_mod.audio.wav.WaveReader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Video sound capture must stop native one-shots when another gun action interrupts them. */
public final class NativeInterruptionExportReview implements ReviewClient.Scenario {
    private GunSoundInstance sound;
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        var capture=BBSModClient.getMinecraftSoundCapture(); if(tick==0) capture.begin(); capture.captureFrame();
        if (tick==10) sound=SoundPlayManager.playAnimationSound(client.player,new ResourceLocation("tacz","ak47/ak47_reload_mvt"),0.8f,1,30);
        if (tick==14) { ReviewClient.require(sound!=null,"Native interrupted sound was not created"); sound.setStop(); }
        if (tick==35) {
            capture.end(); var row=capture.getSounds().stream().filter(s->s.location.getPath().contains("reload_mvt")).findFirst().orElseThrow();
            System.out.println("[REVIEW native stop export] start frame="+row.frame+" stop frame="+row.endFrame);
            var output=Path.of("review-native-interrupted-sound.wav");
            ReviewClient.require(MinecraftSoundMixer.mixToFile(output.toFile(),capture.getSounds(),capture.getFrames(),null,48000,20d,capture.getFrames().size()),"Native video sound mixing failed");
            mchorse.bbs_mod.audio.Wave wave; try (var input=Files.newInputStream(output)) { wave=new WaveReader().read(input); }
            long before=0,after=0; int boundary=(int)Math.ceil(15/20d*wave.sampleRate)*wave.blockAlign;
            for(int i=0;i<wave.data.length;i++) if(wave.data[i]!=0) { if(i<boundary) before++; else after++; }
            System.out.println("[REVIEW native stop export] PCM before/after interruption="+before+"/"+after);
            ReviewClient.require(row.endFrame==15,"Native one-shot interruption was not captured for video export");
            ReviewClient.require(before>100 && after==0,"Exported video keeps playing an interrupted native gun sound"); return true;
        }
        return false;
    }
}
