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
    private boolean guarded;
    public ProjectileReplayReview() { ServerEntityEvents.ENTITY_LOAD.register((entity,world)-> { if(entity instanceof EntityKineticBullet bullet) ProjectileProbe.spawn(bullet); }); }
    @Override protected boolean ctrlFinished(Minecraft client,int elapsed) throws Exception {
        var controller=mchorse.bbs_mod.BBSModClient.getFilms().getController(film.getId());
        if(elapsed==30) {
            System.out.println("[REVIEW projectile clips] ticks="+film.replays.getList().get(0).actions.getClips(TaczProjectileActionClip.class).stream().map(c->c.tick.get()).toList());
            String state=client.getSingleplayerServer().submit(()-> { var a=mchorse.bbs_mod.BBSMod.getActions().getPlayer(film.getId()); return a==null?"none":a.tick+":"+a.playing+":"+a.type+":"+a.film.replays.getList().get(0).actions.getClips(TaczProjectileActionClip.class).stream().map(c->c.tick.get()).toList(); }).get();
            System.out.println("[REVIEW server playback] "+state);
        }
        return (ProjectileProbe.count("ctrl")>=successful && (controller==null || controller.getTick()>=film.calculateDuration()-3)) || elapsed>80;
    }
    @Override protected void act(Minecraft client,int time) {
        if(time==1) {
            IGun.getIGunOrNull(client.player.getMainHandItem()).setFireMode(client.player.getMainHandItem(),FireMode.SEMI);
            var id=client.player.getUUID(); client.getSingleplayerServer().execute(()-> { var p=client.getSingleplayerServer().getPlayerList().getPlayer(id); IGun.getIGunOrNull(p.getMainHandItem()).setFireMode(p.getMainHandItem(),FireMode.SEMI); });
        }
        if (time==5 || time==15 || time==25 || time==35) {
            int index=(time-5)/10; client.player.setYRot(new float[]{0,90,179,-179}[index]); client.player.setXRot(new float[]{-30,0,30,-60}[index]);
            client.player.setYHeadRot(client.player.getYRot()); client.player.setYBodyRot(client.player.getYRot());
            client.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(client.player.getYRot(),client.player.getXRot(),client.player.onGround()));
            var result=IClientPlayerGunOperator.fromLocalPlayer(client.player).shoot();
            System.out.println("[REVIEW projectile] live shoot="+result+" yaw="+client.player.getYRot()+" pitch="+client.player.getXRot());
            ReviewClient.require(result==ShootResult.SUCCESS,"Native trigger pull was rejected: "+result); successful++;
        }
    }
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        ProjectileProbe.phase=phase==2?"record":phase==4||phase==5||phase==3&&tick-phaseTick>=20?"ctrl":phase==7||phase==6&&tick-phaseTick>=10?"f6":"ignore";
        if((phase==4||phase==5)&&!guarded&&mcaxolotl.bbstacz.client.FirstPersonPlayback.ownsPlayer()) {
            var live=IClientPlayerGunOperator.fromLocalPlayer(client.player).shoot();
            System.out.println("[REVIEW projectile ownership] live shoot during owned Ctrl="+live);
            ReviewClient.require(live!=ShootResult.SUCCESS,"Live gun input can add extra projectiles to an owned first-person replay"); guarded=true;
        }
        return super.tick(client,tick);
    }
    @Override protected void verifyCtrl(Minecraft client) throws Exception {
        var actions=film.replays.getList().get(0).actions;
        long pellets=actions.getClips(TaczProjectileActionClip.class).size(), fires=actions.getClips(TaczFireActionClip.class).size();
        System.out.println("[REVIEW projectile] trigger pulls="+successful+" clips="+pellets+" fire clips="+fires+" native spawned record/ctrl="+ProjectileProbe.count("record")+"/"+ProjectileProbe.count("ctrl")+" draws="+ProjectileProbe.rendered+" duplicates="+ProjectileProbe.duplicateDraws);
        ReviewClient.require(successful==4 && pellets==ProjectileProbe.count("record") && fires==4,"Recorded shot/pellet counts disagree with native firing");
        double poseEnd=((mcaxolotl.bbstacz.recording.TaczReplayChannels)film.replays.getList().get(0).keyframes).bbstacz$state().getLength()+1;
        ReviewClient.require(actions.getClips(TaczProjectileActionClip.class).stream().allMatch(c->c.tick.get()<poseEnd),"TACZ action stamps drifted beyond the recorded pose cursor");
        ReviewClient.require(ProjectileProbe.count("ctrl")==pellets,"Ctrl spawned repeated or missing native bullet entities");
        ReviewClient.require(guarded,"No owned Ctrl frame was checked for live input isolation");
        client.getSingleplayerServer().submit(()-> {
            var server=client.getSingleplayerServer(); var player=server.getPlayerList().getPlayer(client.player.getUUID());
            mchorse.bbs_mod.BBSMod.getActions().stop(film.getId());
            var saved=mchorse.bbs_mod.BBSMod.getFilms().load(film.getId());
            var action=mchorse.bbs_mod.BBSMod.getActions().play(player,server.overworld(),saved,0);
            action.playing=false; action.tick=saved.calculateDuration()-2;
            var rp=saved.replays.getList().get(0);
            String previous=ProjectileProbe.phase; ProjectileProbe.phase="seek";
            try {
                for(var clip:rp.actions.getClips(TaczProjectileActionClip.class)) clip.applyAction(player,mchorse.bbs_mod.actions.SuperFakePlayer.get(server.overworld()),saved,rp,clip.tick.get());
                long before=ProjectileProbe.count("seek"); action.goTo(0);
                System.out.println("[REVIEW projectile seek] entities before/after rewind="+before+"/"+ProjectileProbe.count("seek"));
                ReviewClient.require(before==4 && ProjectileProbe.count("seek")==before,"Rewinding a film re-fired transient projectiles in different directions");
                for(var entity:server.overworld().getAllEntities()) if(entity instanceof EntityKineticBullet&&!entity.isRemoved()) throw new AssertionError("Seek kept stale replay projectiles flying");
            } finally { ProjectileProbe.phase=previous; mchorse.bbs_mod.BBSMod.getActions().stop(film.getId()); }
        }).get();
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
