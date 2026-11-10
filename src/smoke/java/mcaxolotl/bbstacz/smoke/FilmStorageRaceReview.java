package mcaxolotl.bbstacz.smoke;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.film.Film;
import net.minecraft.client.Minecraft;

/** Two native gzip writers race a native film reader, only in this review's generated world. */
public final class FilmStorageRaceReview implements ReviewClient.Scenario {
    private CompletableFuture<Void> finished;
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick==0) {
            var manager=BBSMod.getFilms(); String id="bbs-tacz-storage-race-"+System.currentTimeMillis();
            var film=new Film(); film.setId(id); var data=film.toData().asMap();
            byte[] noise=new byte[16384]; new java.util.Random(1).nextBytes(noise);
            data.putString("review_padding",java.util.Base64.getEncoder().encodeToString(noise));
            ReviewClient.require(manager.save(id,data),"Initial native film save failed");
            var pool=Executors.newFixedThreadPool(3,r->{var t=new Thread(r,"BBS TACZ storage review");t.setDaemon(true);return t;});
            var first=CompletableFuture.runAsync(()-> {for(int i=0;i<60;i++) ReviewClient.require(manager.save(id,filmData(data)),"Concurrent native film save failed");},pool);
            var second=CompletableFuture.runAsync(()-> {for(int i=0;i<60;i++) ReviewClient.require(manager.save(id,filmData(data)),"Concurrent retry save failed");},pool);
            var reads=CompletableFuture.runAsync(()-> {for(int i=0;i<160;i++) ReviewClient.require(manager.load(id)!=null,"Concurrent native film read observed a partial gzip");},pool);
            finished=CompletableFuture.allOf(first,second,reads).whenComplete((v,e)->pool.shutdown());
        }
        if(finished.isDone()) { finished.join(); System.out.println("[REVIEW film IO] 120 native saves + 160 native reads without partial gzip PASS"); return true; }
        ReviewClient.require(tick<500,"Concurrent native film IO stalled"); return false;
    }
    private static mchorse.bbs_mod.data.types.MapType filmData(mchorse.bbs_mod.data.types.MapType data) {
        return data.copy().asMap();
    }
}
