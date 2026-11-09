package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.EntityKineticBullet;
import java.nio.file.Path;
import mcaxolotl.bbstacz.actions.TaczFireActionClip;
import mcaxolotl.bbstacz.actions.TaczProjectileActionClip;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.client.Minecraft;

/** Native trigger pulls at multiple yaw/pitch angles through Alt/Ctrl/F6. */
public final class ProjectileReplayReview extends HotkeyWorkflowReview {
    private int successful;
    public ProjectileReplayReview() { ServerEntityEvents.ENTITY_LOAD.register((entity,world)-> { if(entity instanceof EntityKineticBullet bullet) ProjectileProbe.spawn(bullet); }); }
    @Override protected boolean ctrlFinished(Minecraft client,int elapsed) {
        var controller=mchorse.bbs_mod.BBSModClient.getFilms().getController(film.getId());
        return (ProjectileProbe.count("ctrl")>=successful && (controller==null || controller.getTick()>=film.calculateDuration()-3)) || elapsed>80;
    }
    @Override protected void act(Minecraft client,int time) {
        if(time==1) {
            IGun.getIGunOrNull(client.player.getMainHandItem()).setFireMode(client.player.getMainHandItem(),FireMode.SEMI);
            var id=client.player.getUUID(); client.getSingleplayerServer().execute(()-> { var p=client.getSingleplayerServer().getPlayerList().getPlayer(id); IGun.getIGunOrNull(p.getMainHandItem()).setFireMode(p.getMainHandItem(),FireMode.SEMI); });
        }
        if (time==5 || time==15 || time==25 || time==35) {
            int index=(time-5)/10; client.player.setYRot(new float[]{0,90,179,-179}[index]); client.player.setXRot(new float[]{-30,0,30,-60}[index]);
            var result=IClientPlayerGunOperator.fromLocalPlayer(client.player).shoot();
            System.out.println("[REVIEW projectile] live shoot="+result+" yaw="+client.player.getYRot()+" pitch="+client.player.getXRot());
            ReviewClient.require(result==ShootResult.SUCCESS,"Native trigger pull was rejected: "+result); successful++;
        }
    }
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        ProjectileProbe.phase=phase==2?"record":phase==4||phase==5||phase==3&&tick-phaseTick>=20?"ctrl":phase==7||phase==6&&tick-phaseTick>=10?"f6":"ignore";
        return super.tick(client,tick);
    }
    @Override protected void verifyCtrl(Minecraft client) throws Exception {
        var actions=film.replays.getList().get(0).actions;
        long pellets=actions.getClips(TaczProjectileActionClip.class).size(), fires=actions.getClips(TaczFireActionClip.class).size();
        System.out.println("[REVIEW projectile] trigger pulls="+successful+" clips="+pellets+" fire clips="+fires+" native spawned record/ctrl="+ProjectileProbe.count("record")+"/"+ProjectileProbe.count("ctrl")+" draws="+ProjectileProbe.rendered+" duplicates="+ProjectileProbe.duplicateDraws);
        ReviewClient.require(successful==4 && pellets==ProjectileProbe.count("record") && fires==4,"Recorded shot/pellet counts disagree with native firing");
        ReviewClient.require(ProjectileProbe.count("ctrl")==pellets,"Ctrl spawned repeated or missing native bullet entities");
        var live=IClientPlayerGunOperator.fromLocalPlayer(client.player).shoot();
        System.out.println("[REVIEW projectile ownership] live shoot during Ctrl="+live);
        ReviewClient.require(live!=ShootResult.SUCCESS,"Live gun input can add extra projectiles to an owned first-person replay");
        long before=ProjectileProbe.count("ctrl");
        client.getSingleplayerServer().submit(()-> {
            var action=mchorse.bbs_mod.BBSMod.getActions().getPlayer(film.getId()); action.playing=false; action.goTo(0);
            for(var entity:client.getSingleplayerServer().overworld().getAllEntities())
                if(entity instanceof EntityKineticBullet && !entity.isRemoved()) throw new AssertionError("Seek kept stale replay projectiles flying");
        }).get();
        System.out.println("[REVIEW projectile seek] entities before/after rewind="+before+"/"+ProjectileProbe.count("ctrl"));
        ReviewClient.require(ProjectileProbe.count("ctrl")==before,"Rewinding a film re-fired transient projectiles in different directions");
    }
    @Override protected void verifyVideo(Minecraft client,Path video) {
        long pellets=film.replays.getList().get(0).actions.getClips(TaczProjectileActionClip.class).size();
        ReviewClient.require(ProjectileProbe.count("f6")==pellets,"F6 spawned repeated or missing native bullet entities");
        ReviewClient.require(ProjectileProbe.rendered>0 && ProjectileProbe.duplicateDraws==0,"Native tracers were missing or rendered repeatedly in a single frame");
        var replay=ProjectileProbe.shots.stream().filter(s->s.phase().equals("ctrl")||s.phase().equals("f6")).toList();
        for (var shot:replay) {
            float expectedYaw=(float)Math.toDegrees(Math.atan2(shot.velocity().x,shot.velocity().z));
            float expectedPitch=(float)Math.toDegrees(Math.atan2(shot.velocity().y,Math.hypot(shot.velocity().x,shot.velocity().z)));
            ReviewClient.require(Math.abs(net.minecraft.util.Mth.wrapDegrees(shot.yaw()-expectedYaw))<0.01 && Math.abs(shot.pitch()-expectedPitch)<0.01,"Tracer facing differs from its actual velocity");
            ReviewClient.require(Math.abs(net.minecraft.util.Mth.wrapDegrees(shot.oldYaw()-shot.yaw()))<0.01 && Math.abs(shot.oldPitch()-shot.pitch())<0.01,"Replayed bullet interpolates from an unrelated direction on its first frame");
        }
        System.out.println("[REVIEW projectile] F6 pellets="+ProjectileProbe.count("f6")+" render calls="+ProjectileProbe.rendered+" duplicates="+ProjectileProbe.duplicateDraws);
    }
}
