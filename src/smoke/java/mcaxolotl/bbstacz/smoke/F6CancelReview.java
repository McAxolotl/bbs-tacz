package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.item.builder.GunItemBuilder;
import java.util.List;
import java.util.Map;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.PoseMath;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.WorldVideoExportSession;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.ContentType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Cancel before the play response, cancel/retry, then cancel an already-created controller. */
public final class F6CancelReview implements ReviewClient.Scenario {
    private Film film;
    private WorldVideoExportSession export;
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick==0) {
            var gun=GunItemBuilder.create().setId(new ResourceLocation("tacz","ak47")).setAmmoCount(30).setAmmoInBarrel(true).build();
            var frame=new FrameSnapshot(gun.save(new net.minecraft.nbt.CompoundTag()).toString(),true,Map.of(),Map.of(),PoseMath.identity(),false,false,0,0,-1,1000,0,List.of(),new float[]{0,0,0,1},List.of());
            film=new Film(); film.setId("f6-cancel-review-"+System.currentTimeMillis()); var replay=new Replay("cancel_player"); replay.form.set(new TaczForm()); replay.fp.set(true); film.replays.add(replay);
            var state=((TaczReplayChannels)replay.keyframes).bbstacz$state(); state.insert(0,frame); state.insert(200,frame);
            replay.keyframes.x.insert(0,client.player.getX()); replay.keyframes.y.insert(0,client.player.getY()); replay.keyframes.z.insert(0,client.player.getZ());
            ContentType.FILMS.getRepository().save(film.getId(),(mchorse.bbs_mod.data.types.MapType)film.toData());
            BBSSettings.videoDelay.set(0.8f); BBSSettings.videoExportAudio.set(false); BBSSettings.videoExportMinecraftSounds.set(false); BBSSettings.worldExportResizeWindow.set(false);
            var field=BBSModClient.class.getDeclaredField("worldExportSession"); field.setAccessible(true); export=(WorldVideoExportSession)field.get(null);
        }
        if(tick==5) { ReviewClient.require(export.start(film.getId(),film),"Export did not start"); export.cancel(); }
        if(tick==25) {
            boolean server=client.getSingleplayerServer().submit(()->BBSMod.getActions().getPlayer(film.getId())!=null).get(5,java.util.concurrent.TimeUnit.SECONDS);
            System.out.println("[REVIEW F6 cancel] after immediate cancel: client="+BBSModClient.getFilms().has(film.getId())+" server="+server);
            ReviewClient.require(!export.isExporting()&&!server&&!BBSModClient.getFilms().has(film.getId())&&FirstPersonPlayback.target(0)==null,"Cancelled F6 request resurrected on its late play response");
        }
        if(tick==26) { export.start(film.getId(),film); export.cancel(); ReviewClient.require(export.start(film.getId(),film),"Cancelled export could not restart"); }
        if(tick==45) { ReviewClient.require(export.isExporting()&&BBSModClient.getFilms().has(film.getId()),"Cancel/retry dropped the new playback instead of the old response"); export.cancel(); }
        if(tick==65) {
            boolean server=client.getSingleplayerServer().submit(()->BBSMod.getActions().getPlayer(film.getId())!=null).get(5,java.util.concurrent.TimeUnit.SECONDS);
            ReviewClient.require(!export.isExporting()&&!server&&!BBSModClient.getFilms().has(film.getId()),"Cancelling an active F6 export left a controller or restarted the server film");
        }
        if(tick==80) { ReviewProbe.failFirstFrame=true; ReviewClient.require(export.start(film.getId(),film),"Failure-path export could not start"); }
        if(tick==115) {
            boolean server=client.getSingleplayerServer().submit(()->BBSMod.getActions().getPlayer(film.getId())!=null).get(5,java.util.concurrent.TimeUnit.SECONDS);
            ReviewClient.require(!export.isExporting()&&!server&&!BBSModClient.getFilms().has(film.getId())&&!ReviewProbe.failFirstFrame,"Failed first readback left the client or server timeline locked");
            return true;
        }
        return false;
    }
}
