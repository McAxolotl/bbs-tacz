package mcaxolotl.bbstacz.smoke;

import mcaxolotl.bbstacz.forms.TaczForm;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Native tick/disconnect/stop reentrancy, borrowed inventory return and a replacement playback kept alive. */
public final class ActionLifecycleReview implements ReviewClient.Scenario {
    private Film film(String id,boolean fp) {
        Film film=new Film(); film.setId(id);
        Replay replay=new Replay("lifecycle"); replay.fp.set(fp); replay.form.set(new TaczForm()); film.replays.add(replay);
        return film;
    }
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick!=0) return false;
        client.getSingleplayerServer().submit(()-> {
            var server=client.getSingleplayerServer(); var player=server.getPlayerList().getPlayer(client.player.getUUID());
            var manager=BBSMod.getActions(); String id="bbs-tacz-action-lifecycle-"+System.nanoTime();
            player.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));
            var first=film(id,true); manager.play(player,server.overworld(),first,0);
            ActionLifecycleProbe.film=id; ActionLifecycleProbe.tick=()->manager.stopFor(player);
            manager.tick();
            ReviewClient.require(manager.getPlayer(id)==null,"Disconnect left its native action active");
            ReviewClient.require(player.getInventory().getItem(0).is(Items.DIAMOND)&&player.getInventory().getItem(0).getCount()==7,"Disconnect did not return borrowed equipment");
            String replacement=id+"-replacement";
            manager.play(player,server.overworld(),first,0);
            ActionLifecycleProbe.stop=()->manager.play(player,server.overworld(),film(replacement,false),0);
            manager.stop(id);
            ReviewClient.require(manager.getPlayer(id)==null&&manager.getPlayer(replacement)!=null,"Stop callback lost the newly created playback");
            manager.stop(replacement);
            ReviewClient.require(player.getInventory().getItem(0).is(Items.DIAMOND)&&player.getInventory().getItem(0).getCount()==7,"Stop did not return borrowed equipment");
            System.out.println("[REVIEW action lifecycle] native disconnect/tick and replacement/stop callbacks; borrowed equipment restored; backend="+manager.getClass().getSimpleName());
        }).get();
        return true;
    }
}
