package mcaxolotl.bbstacz.recording;

import com.tacz.guns.entity.EntityKineticBullet;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.film.Film;

/** Only replay-spawned projectiles belong to this lifetime; live guns and other films are never globally cleared. */
public final class ReplayProjectiles {
    private static final WeakIdentityMap<Film,List<WeakReference<EntityKineticBullet>>> OWNED=new WeakIdentityMap<>();
    private ReplayProjectiles() {}
    public static void add(Film film,EntityKineticBullet bullet) {
        // Direct/native action execution has no film owner; its physical entity must stay live and untracked.
        if(film==null || bullet==null) return;
        var bullets=OWNED.computeIfAbsent(film,key->new ArrayList<>());
        bullets.removeIf(ref->ref.get()==null || ref.get().isRemoved()); bullets.add(new WeakReference<>(bullet));
    }
    public static void clear(Film film) {
        if(film==null) return;
        var bullets=OWNED.remove(film); if(bullets==null) return;
        bullets.forEach(ref->{ var bullet=ref.get(); if(bullet!=null&&!bullet.isRemoved()) bullet.discard(); });
    }
    public static void clearAll() { OWNED.keys().forEach(ReplayProjectiles::clear); }
}
