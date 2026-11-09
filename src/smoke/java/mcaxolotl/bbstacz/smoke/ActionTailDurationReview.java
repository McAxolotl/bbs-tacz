package mcaxolotl.bbstacz.smoke;

import mcaxolotl.bbstacz.actions.TaczProjectileActionClip;
import mcaxolotl.bbstacz.forms.TaczForm;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import net.minecraft.client.Minecraft;

/** A native projectile past the pose tail must remain in the serialized/exported film's actual duration. */
public final class ActionTailDurationReview implements ReviewClient.Scenario {
    @Override public boolean tick(Minecraft client,int tick) {
        var film=new Film(); var replay=new Replay("player"); replay.form.set(new TaczForm()); replay.keyframes.x.insert(5,0d); film.replays.add(replay);
        var shot=new TaczProjectileActionClip(); shot.tick.set(20); shot.duration.set(1); replay.actions.addClip(shot);
        ReviewClient.require(film.calculateDuration()==21,"Film duration truncates a real native action after the final pose key");
        var copy=new Film(); copy.fromData(film.toData()); ReviewClient.require(copy.calculateDuration()==21,"Serialized native action tail was lost");
        shot.enabled.set(false); ReviewClient.require(film.calculateDuration()==6,"Disabled native action unexpectedly extended the film");
        System.out.println("[REVIEW action duration] pose end=6; native shot end=21; saved end=21; disabled end=6"); return true;
    }
}
