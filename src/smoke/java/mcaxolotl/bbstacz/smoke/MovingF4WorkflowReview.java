package mcaxolotl.bbstacz.smoke;

import java.nio.file.Files;
import java.nio.file.Path;
import mcaxolotl.bbstacz.forms.TaczForm;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Real-time F4 during all six keyboard motion + accepted action phases. */
public final class MovingF4WorkflowReview extends MovingGunWorkflowReview {
    private Path output; private int stage,start;
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick==0) {
            client.options.setCameraType(CameraType.FIRST_PERSON); Morph.getMorph(client.player).setForm(new TaczForm());
            var gun=com.tacz.guns.api.item.builder.GunItemBuilder.create().setId(new net.minecraft.resources.ResourceLocation("tacz","ak47")).setAmmoCount(30).setAmmoInBarrel(true).setFireMode(com.tacz.guns.api.item.gun.FireMode.SEMI).build();
            client.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,gun); com.tacz.guns.resource.modifier.AttachmentPropertyManager.postChangeEvent(client.player,gun);
            var uuid=client.player.getUUID(); client.getSingleplayerServer().submit(()-> {var p=client.getSingleplayerServer().getPlayerList().getPlayer(uuid); p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,gun.copy()); com.tacz.guns.resource.modifier.AttachmentPropertyManager.postChangeEvent(p,gun);}).get();
            output=Path.of("review-moving-f4-"+System.currentTimeMillis()).toAbsolutePath(); Files.createDirectories(output); F4ClockReview.configure(output,30);
        }
        if(stage==0&&tick>40) { F4ClockReview.key(GLFW.GLFW_KEY_F4); stage=1; }
        if(stage==1&&BBSModClient.getVideoRecorder().isRecording()) { stage=2; start=tick; ReviewProbe.resetClock(); ProjectileProbe.phase="f4"; }
        if(stage==2) { act(client,tick-start); if(tick-start>=recordTicks()) { F4ClockReview.key(GLFW.GLFW_KEY_F4); stage=3; } }
        if(stage==3&&!BBSModClient.getVideoRecorder().isRecording()) {
            var movies=F4ClockReview.movies(output); ReviewClient.require(movies.size()==1&&Files.size(movies.get(0))>1000,"Moving F4 exported no video");
            ReviewClient.require(completedMotions()==6&&realShots()==6&&ReviewProbe.serverSuccessfulShots>=6,"Moving F4 did not execute every real input/action phase");
            double world=(ReviewProbe.lastWorldTime-ReviewProbe.firstWorldTime)/20d,gun=(ReviewProbe.lastGunTime-ReviewProbe.firstGunTime)/1e9; ReviewClient.require(Math.abs(world-gun)<0.02,"Moving F4 clocks differ");
            ReviewClient.require(ProjectileProbe.duplicateDraws==0&&ProjectileProbe.draws("f4")>0,"Moving F4 tracer rendering missing/duplicated");
            writeMotionEvidence();
            Files.writeString(Path.of("bbs-tacz-review-"+getClass().getSimpleName()+"-video.txt"),movies.get(0)+"\n");
            System.out.println("[REVIEW moving F4] modes="+completedMotions()+" shots="+realShots()+" world/gun="+world+"/"+gun+" tracer duplicates="+ProjectileProbe.duplicateDraws+" video="+movies.get(0)); ReviewProbe.playerDraw=null; return true;
        }
        return false;
    }
}
