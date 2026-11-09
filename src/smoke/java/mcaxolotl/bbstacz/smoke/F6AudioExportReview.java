package mcaxolotl.bbstacz.smoke;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import mcaxolotl.bbstacz.forms.TaczForm;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.Link;
import net.minecraft.client.Minecraft;

/** Actual F6 with selected replay skin, film audio and native Minecraft-sound post-mixing enabled. */
public final class F6AudioExportReview extends F6ExportReview {
    @Override protected boolean withAudio() { return true; }
    @Override protected void configureForm(TaczForm form) throws Exception {
        String path="skins/review-f6-magenta-"+System.currentTimeMillis()+".png";
        Files.createDirectories(BBSMod.getAssetsPath(path).toPath().getParent());
        try(var skin=new NativeImage(64,64,false)) { for(int y=0;y<64;y++) for(int x=0;x<64;x++) skin.setPixelRGBA(x,y,0xffff00ff); skin.writeToFile(BBSMod.getAssetsPath(path).toPath()); }
        form.texture.set(Link.assets(path));
    }
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(!super.tick(client,tick)) return false;
        String video=Files.readString(Path.of("bbs-tacz-review-F6AudioExportReview-video.txt")).trim();
        byte[] info=run("/usr/bin/ffprobe","-v","error","-select_streams","a:0","-show_entries","stream=codec_name","-of","csv=p=0",video);
        ReviewClient.require(new String(info,StandardCharsets.UTF_8).contains("aac"),"F6 did not mux the recorded TACZ audio track");
        byte[] audio=run("/usr/bin/ffmpeg","-v","error","-i",video,"-map","0:a:0","-f","s16le","-");
        long audible=0; for(byte b:audio) if(b!=0) audible++;
        ReviewClient.require(audible>1000,"Exported AAC stream decodes to silence");
        int selected=0;
        for(String time:new String[]{"0.5","1","1.5","2"}) {
            byte[] png=run("/usr/bin/ffmpeg","-v","error","-ss",time,"-i",video,"-frames:v","1","-f","image2pipe","-vcodec","png","-");
            if(png.length==0) continue;
            int pixels=0;
            try(var image=NativeImage.read(new ByteArrayInputStream(png))) {
                for(int y=0;y<image.getHeight();y++) for(int x=0;x<image.getWidth();x++) { int c=image.getPixelRGBA(x,y); if((c&255)>80&&((c>>8)&255)<70&&((c>>16)&255)>80) pixels++; }
                if(pixels>selected) { selected=pixels; image.writeToFile(Path.of("review-f6-selected-skin.png")); }
            }
        }
        ReviewClient.require(selected>100,"F6 hands did not use the replay's selected skin (pixels="+selected+")");
        System.out.println("[REVIEW F6 audio] AAC nonzero bytes="+audible+" replay-skin pixels="+selected);
        return true;
    }
    private static byte[] run(String... command) throws Exception {
        var process=new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        byte[] output=process.getInputStream().readAllBytes();
        ReviewClient.require(process.waitFor()==0,"Media verification failed: "+command[0]);
        return output;
    }
}
