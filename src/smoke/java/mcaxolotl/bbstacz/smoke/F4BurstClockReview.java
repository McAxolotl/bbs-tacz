package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.util.time.GunTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.lwjgl.glfw.GLFW;

/** Slow render frames must not collapse a native burst's real-time scheduler into one video instant. */
public final class F4BurstClockReview implements ReviewClient.Scenario {
    private final ArrayList<Long> fireTimes=new ArrayList<>();
    private Path output;
    private int stage,frames;
    private long interval;
    public F4BurstClockReview() { GunFireEvent.CALLBACK.register(event-> {
        if(event.getLogicalSide().isClient()&&event.getShooter()==Minecraft.getInstance().player&&BBSModClient.getVideoRecorder().isRecording()) fireTimes.add(GunTime.nowMillis());
    }); }
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if (tick==0) {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            var id=new ResourceLocation("tacz","b93r"); var gun=GunItemBuilder.create().setId(id).setFireMode(FireMode.BURST).setAmmoCount(20).setAmmoInBarrel(true).build();
            interval=TimelessAPI.getCommonGunIndex(id).orElseThrow().getGunData().getBurstShootInterval();
            client.player.setItemSlot(EquipmentSlot.MAINHAND,gun); AttachmentPropertyManager.postChangeEvent(client.player,gun);
            var playerId=client.player.getUUID(); client.getSingleplayerServer().execute(()-> { var p=client.getSingleplayerServer().getPlayerList().getPlayer(playerId); p.setItemSlot(EquipmentSlot.MAINHAND,gun.copy()); AttachmentPropertyManager.postChangeEvent(p,gun); });
            output=Path.of("review-f4-burst-"+System.currentTimeMillis()).toAbsolutePath(); Files.createDirectories(output); F4ClockReview.configure(output,30);
            BBSSettings.videoHeldFrames.set(1);
        }
        if (stage==0&&tick>30&&ReviewProbe.liveGunDraws>8) { F4ClockReview.key(GLFW.GLFW_KEY_F4); stage=1; ReviewProbe.exportSleepMillis=160; }
        frames=Math.max(frames,BBSModClient.getVideoRecorder().getCounter());
        if (stage==1 && BBSModClient.getVideoRecorder().isRecording() && frames>=3) {
            var result=IClientPlayerGunOperator.fromLocalPlayer(client.player).shoot(); ReviewClient.require(result==ShootResult.SUCCESS,"Native burst was rejected: "+result); stage=2;
        }
        if (stage==2 && fireTimes.size()>=3 && frames>=14) { F4ClockReview.key(GLFW.GLFW_KEY_F4); stage=3; ReviewProbe.exportSleepMillis=0; }
        if (stage==3&&!BBSModClient.getVideoRecorder().isRecording()) {
            System.out.println("[REVIEW F4 burst] native interval="+interval+" fire video times="+fireTimes+" frames="+frames);
            ReviewClient.require(F4ClockReview.movies(output).size()==1,"F4 burst exported no video");
            ReviewClient.require(fireTimes.size()==3,"Native burst emitted wrong number of client fire effects");
            for(int i=1;i<fireTimes.size();i++) ReviewClient.require(Math.abs((fireTimes.get(i)-fireTimes.get(i-1))-interval)<=35,"Native burst scheduling still uses wall time instead of video time");
            return true;
        }
        ReviewClient.require(tick<250,"Native burst clock review stalled"); return false;
    }
}
