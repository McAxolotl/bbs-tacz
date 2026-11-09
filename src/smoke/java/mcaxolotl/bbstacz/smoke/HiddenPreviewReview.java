package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.item.builder.GunItemBuilder;
import java.util.List;
import java.util.Map;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.PoseMath;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** A loaded but hidden editor tab is not a world playback and must not hijack the live first-person camera. */
public final class HiddenPreviewReview implements ReviewClient.Scenario {
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick!=10) return false;
        var gun=GunItemBuilder.create().setId(new ResourceLocation("tacz","ak47")).setAmmoCount(30).setAmmoInBarrel(true).build();
        var frame=new FrameSnapshot(gun.save(new net.minecraft.nbt.CompoundTag()).toString(),true,Map.of(),Map.of(),PoseMath.identity(),true,false,0,0,-1,1000,0,List.of(),new float[]{0,0,0,1},List.of());
        Film film=new Film(); film.setId("hidden-preview-review"); var replay=new Replay("editor_player"); replay.form.set(new TaczForm()); replay.fp.set(true); film.replays.add(replay);
        var channel=((TaczReplayChannels)replay.keyframes).bbstacz$state(); channel.insert(0,frame); channel.insert(200,frame);
        var panel=BBSModClient.getDashboard().getPanel(UIFilmPanel.class); panel.fill(film); client.setScreen(null);
        var target=FirstPersonPlayback.target(0);
        System.out.println("[REVIEW hidden preview] camera owner="+(target==null?"none":target.controller().getClass().getSimpleName()));
        ReviewClient.require(target==null,"A hidden film editor still hijacks the live FP camera");
        var dashboard=BBSModClient.getDashboard(); dashboard.getPanels().setPanel(panel); mchorse.bbs_mod.ui.framework.UIScreen.open(dashboard);
        var first=FirstPersonPlayback.target(0.2f); var second=FirstPersonPlayback.target(0.8f);
        ReviewClient.require(first!=null&&second!=null&&first.tick()==second.tick(),"Visible paused editor preview is missing or advances with render delta");
        client.setScreen(null);
        ReviewClient.require(FirstPersonPlayback.target(0)==null,"Closing the editor left a stale FP camera override");
        return true;
    }
}
