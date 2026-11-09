package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.item.builder.GunItemBuilder;
import java.util.List;
import java.util.Map;
import mcaxolotl.bbstacz.client.FirstPersonPlayback;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.PhysicalCrawl;
import mcaxolotl.bbstacz.recording.PoseMath;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FirstPersonFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Stopping an unrelated controller must not stand up a player still owned by a crawling FP film. */
public final class CrawlOwnershipReview implements ReviewClient.Scenario {
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick!=10) return false;
        PhysicalCrawl.release(client.player);
        var gun=GunItemBuilder.create().setId(new ResourceLocation("tacz","ak47")).setAmmoCount(30).setAmmoInBarrel(true).build();
        var frame=new FrameSnapshot(gun.save(new net.minecraft.nbt.CompoundTag()).toString(),true,Map.of(),Map.of(),PoseMath.identity(),true,true,0,0,-1,1000,0,List.of(),new float[]{0,0,0,1},List.of());
        Film film=new Film(); film.setId("crawl-owner-review"); var replay=new Replay("crawling_player"); replay.form.set(new TaczForm()); replay.fp.set(true); film.replays.add(replay);
        var channel=((TaczReplayChannels)replay.keyframes).bbstacz$state(); channel.insert(0,frame); channel.insert(200,frame);
        var active=new FirstPersonFilmController(film); active.paused=true; BBSModClient.getFilms().add(active); FilmEvents.TICK_AFTER.invoker().onFilmTick(active,0);
        ReviewClient.require(PhysicalCrawl.isCrawling(client.player),"Active FP film did not apply crawling");
        float before=client.player.getBbHeight();
        Film unrelated=new Film(); unrelated.setId("unrelated-review"); var other=new FirstPersonFilmController(unrelated); other.shutdown();
        System.out.println("[REVIEW crawl] active target="+(FirstPersonPlayback.target(0)!=null)+" before="+before+" after unrelated shutdown="+client.player.getBbHeight());
        ReviewClient.require(PhysicalCrawl.isCrawling(client.player)&&client.player.getBbHeight()<0.7f,"Unrelated controller shutdown released the active film's crawling pose");
        Object temporary=new Object(); PhysicalCrawl.apply(client.player,temporary,false);
        ReviewClient.require(!PhysicalCrawl.isCrawling(client.player),"Latest explicit false owner did not win");
        PhysicalCrawl.release(client.player,temporary);
        ReviewClient.require(PhysicalCrawl.isCrawling(client.player),"Releasing one override lost the surviving owner");
        var id=client.player.getUUID();
        boolean server=client.getSingleplayerServer().submit(()->{
            var player=client.getSingleplayerServer().getPlayerList().getPlayer(id); Object a=new Object(),b=new Object();
            PhysicalCrawl.apply(player,a,true); PhysicalCrawl.apply(player,b,false); PhysicalCrawl.release(player,b);
            boolean kept=PhysicalCrawl.isCrawling(player)&&player.getBbHeight()<0.7f;
            PhysicalCrawl.release(player,a); return kept&&!PhysicalCrawl.isManaged(player);
        }).get(5,java.util.concurrent.TimeUnit.SECONDS);
        ReviewClient.require(server,"Server crawl owners were not independent or failed to restore");
        BBSModClient.getFilms().remove(film.getId());
        ReviewClient.require(!PhysicalCrawl.isManaged(client.player)&&!PhysicalCrawl.isCrawling(client.player)&&FirstPersonPlayback.target(0)==null,"Stopping the owner left stale FP/crawl state");
        return true;
    }
}
