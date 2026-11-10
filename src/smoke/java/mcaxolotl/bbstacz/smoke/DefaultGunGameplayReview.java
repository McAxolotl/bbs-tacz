package mcaxolotl.bbstacz.smoke;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.input.ShootKey;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.NativeSoundTimeline;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Every loaded stock gun/fire-mode: actual moving ShootKey, native charges/heat, inspect/refit/melee,
 * client + server acceptance, evaluated FP pose and native sound serialization. No direct animation triggers. */
public final class DefaultGunGameplayReview implements ReviewClient.Scenario {
    private record Work(ResourceLocation gun,FireMode mode,GunData data) {}
    private final List<Work> work=new ArrayList<>();
    private final List<Map<String,Object>> rows=new ArrayList<>();
    private int cursor=-1,start,stage,stageTick,uses,fires,melees,useStart,fireStart,meleeStart,serverStart,views;
    private boolean chargeSeen,heatSeen,heatPrepared;
    private double x,z,distance;
    private final Map<String,Integer> results=new java.util.HashMap<>();
    private Film film; private ItemStack stack;
    public DefaultGunGameplayReview() {
        ReviewProbe.shootResult=result->results.merge(result.name(),1,Integer::sum);
        com.tacz.guns.api.event.common.GunShootEvent.CALLBACK.register(e->{if(e.getLogicalSide().isClient()) uses++;});
        com.tacz.guns.api.event.common.GunFirePostEvent.EVENT.register(e->{if(e.getLogicalSide().isClient()) fires++;});
        com.tacz.guns.api.event.common.GunMeleeEvent.CALLBACK.register(e->{if(e.getLogicalSide().isClient()) melees++;});
    }
    @Override public int timeoutTicks() { return 21000; }
    private void begin(Minecraft client,int tick) {
        start=tick; stage=0; results.clear(); chargeSeen=heatSeen=heatPrepared=false; views=0; distance=0;
        useStart=uses; fireStart=fires; meleeStart=melees; serverStart=ReviewProbe.serverSuccessfulShots;
        Work current=work.get(cursor);
        stack=GunItemBuilder.create().setId(current.gun()).setAmmoCount(current.data().getAmmoAmount()).setAmmoInBarrel(true).setFireMode(current.mode()).build();
        ItemStack old=client.player.getMainHandItem(); client.player.setItemSlot(EquipmentSlot.MAINHAND,stack); AttachmentPropertyManager.postChangeEvent(client.player,stack);
        var uuid=client.player.getUUID(); client.getSingleplayerServer().submit(()-> {var p=client.getSingleplayerServer().getPlayerList().getPlayer(uuid); p.setItemSlot(EquipmentSlot.MAINHAND,stack.copy()); AttachmentPropertyManager.postChangeEvent(p,p.getMainHandItem());}).join();
        IClientPlayerGunOperator.fromLocalPlayer(client.player).draw(old);
        client.player.setYRot(cursor*37); client.player.setXRot(80); x=client.player.getX(); z=client.player.getZ();
        film=new Film(); film.setId("bbs-tacz-default-gameplay-"+cursor+"-"+System.currentTimeMillis()); var replay=new Replay("native_player"); replay.fp.set(true); replay.form.set(new TaczForm()); film.replays.add(replay);
        mchorse.bbs_mod.ui.ContentType.FILMS.getRepository().save(film.getId(),film.toData().asMap());
        BBSModClient.getFilms().startRecording(film,0,0,false); BBSModClient.getFilms().getRecorder().countdown=0;
        client.options.keyUp.setDown(cursor%3==0); client.options.keyDown.setDown(cursor%3==1); client.options.keyRight.setDown(cursor%3==2); client.options.keyShift.setDown(cursor%5==0);
    }
    @Override public boolean tick(Minecraft client,int tick) throws Exception {
        if(tick==0) {
            Morph.getMorph(client.player).setForm(new TaczForm()); client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            var guns=new ArrayList<>(TimelessAPI.getAllClientGunIndex()); guns.sort(Comparator.comparing(e->e.getKey().toString()));
            for(var gun:guns) if(System.getProperty("bbstacz.review.gun")==null||gun.getKey().toString().equals(System.getProperty("bbstacz.review.gun"))) for(FireMode mode:gun.getValue().getGunData().getFireModeSet()) { ReviewClient.require(mode!=FireMode.UNKNOWN,"Stock gun has no declared fire mode: "+gun.getKey()); work.add(new Work(gun.getKey(),mode,gun.getValue().getGunData())); }
            ReviewClient.require(!work.isEmpty()&&(System.getProperty("bbstacz.review.gun")!=null||work.size()>=guns.size()),"Fire-mode catalog is empty"); System.out.println("[REVIEW gameplay catalog] guns="+guns.size()+" declared gun/mode pairs="+work.size());
        }
        if(tick==35) { cursor=0; begin(client,tick); }
        if(cursor<0) return false;
        Work current=work.get(cursor); int elapsed=tick-start; var op=IClientPlayerGunOperator.fromLocalPlayer(client.player); var item=IGun.getIGunOrNull(client.player.getMainHandItem());
        chargeSeen|=op.isCharging()||op.getChargeProgress()>0;
        heatSeen|=uses>useStart&&fires>fireStart&&item!=null&&item.isOverheatLocked(client.player.getMainHandItem());
        distance=Math.max(distance,Math.hypot(client.player.getX()-x,client.player.getZ()-z));
        if(stage==0&&elapsed>=30) {
            // Heat is a boundary precondition, not hundreds of frame-rate-dependent warm-up shots.
            // The transition to locked must still be produced by new native client/server shots.
            if(current.data().hasHeatData()&&!heatPrepared) {
                heatPrepared=true; float near=current.data().getHeatData().getHeatMax()-1;
                item.setHeatAmount(client.player.getMainHandItem(),near); item.setOverheatLocked(client.player.getMainHandItem(),false);
                var uuid=client.player.getUUID(); client.getSingleplayerServer().submit(()-> {var p=client.getSingleplayerServer().getPlayerList().getPlayer(uuid); var held=p.getMainHandItem(); var gun=IGun.getIGunOrNull(held); gun.setHeatAmount(held,near); gun.setOverheatLocked(held,false);}).join();
            }
            if(elapsed%40==0) System.out.println("[REVIEW stock trigger] requested="+current.gun()+" "+current.mode()+" actual="+(item==null?null:item.getGunId(client.player.getMainHandItem()))+" mode="+(item==null?null:item.getFireMode(client.player.getMainHandItem()))+" results="+results+" screen="+client.screen+" recorded="+op.getDataHolder().isShootRecorded+" draw="+com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(client.player).getSynDrawCoolDown());
            ReviewClient.require(item!=null&&item.getGunId(client.player.getMainHandItem()).equals(current.gun())&&item.getFireMode(client.player.getMainHandItem())==current.mode(),"Native trigger equipped a different gun/mode than requested");
            boolean enough=current.data().hasHeatData()?heatSeen&&uses>useStart&&fires>fireStart:uses>useStart&&fires-fireStart>=(current.mode()==FireMode.BURST?current.data().getBurstData().getCount():current.mode()==FireMode.AUTO?2:1);
            ShootKey.SHOOT_KEY.setDown(!enough);
            if(enough) { stage=1; stageTick=tick; op.inspect(); }
            ReviewClient.require(elapsed<(current.data().hasHeatData()?800:240),"Native moving trigger produced no complete shot sequence: "+current.gun()+" "+current.mode()+" uses="+(uses-useStart)+" fires="+(fires-fireStart)+" lock="+op.getDataHolder().clientStateLock+" results="+results);
        }
        if(stage==1) {
            int age=tick-stageTick;
            if(age==12&&!current.data().getAllowAttachments().isEmpty()) client.setScreen(new GunRefitScreen());
            if(age>=16&&age<20&&client.screen instanceof GunRefitScreen) for(var type:current.data().getAllowAttachments()) if(RefitTransform.changeRefitScreenView(type)) views++;
            if(age==22&&client.screen!=null) client.screen.onClose();
            if(age>=30&&age<140&&melees==meleeStart&&!op.getDataHolder().clientStateLock&&op.getClientShootCoolDown()==0) op.melee();
            boolean nativeMelee=current.data().getMeleeData().getDefaultMeleeData()!=null;
            if(age>=75&&(!nativeMelee||melees>meleeStart)||age>=155) {
                ShootKey.SHOOT_KEY.setDown(false); client.options.keyUp.setDown(false); client.options.keyDown.setDown(false); client.options.keyRight.setDown(false); client.options.keyShift.setDown(false);
                var recorder=BBSModClient.getFilms().stopRecording(); film.replays.getList().get(0).keyframes.copyOver(recorder.keyframes,0);
                var copy=new Film(); copy.fromData(film.toData()); var channels=(TaczReplayChannels)copy.replays.getList().get(0).keyframes;
                ReviewClient.require(distance>0.2,"Stock gun scenario never moved: "+current.gun());
                ReviewClient.require(ReviewProbe.serverSuccessfulShots>serverStart,"Server did not accept the actual stock gun trigger: "+current.gun());
                ReviewClient.require(channels.bbstacz$state().getKeyframes().stream().anyMatch(key->key.getValue().firstPerson()&&!key.getValue().gunBones().isEmpty()),"Stock gun produced no evaluated/serialized FP pose: "+current.gun());
                var cues=NativeSoundTimeline.read(channels.bbstacz$sounds()); ReviewClient.require(!cues.isEmpty(),"Stock gun emitted/serialized no native sound: "+current.gun());
                ReviewClient.require(current.data().getChargeData(current.mode())==null||chargeSeen,"Native stock charge was never exercised: "+current.gun());
                ReviewClient.require(!current.data().hasHeatData()||heatSeen,"Native stock overheating was never reached: "+current.gun());
                ReviewClient.require(!nativeMelee||melees>meleeStart,"Accepted-idle stock melee produced no event: "+current.gun()+" lock="+op.getDataHolder().clientStateLock+" cooldown="+op.getClientShootCoolDown());
                ReviewClient.require(copy.camera.getClips(mchorse.bbs_mod.camera.clips.misc.AudioClip.class).isEmpty(),"Stock gun sounds were extracted to clips");
                var row=new java.util.LinkedHashMap<String,Object>(); row.put("gun",current.gun().toString()); row.put("mode",current.mode().name()); row.put("distance",distance); row.put("client_uses",uses-useStart); row.put("native_fires",fires-fireStart); row.put("server_uses",ReviewProbe.serverSuccessfulShots-serverStart); row.put("native_sound_cues",cues.size()); row.put("charge",current.data().getChargeData(current.mode())==null?"N/A":chargeSeen?"PASS":"FAIL"); row.put("heat",current.data().hasHeatData()?heatSeen?"PASS":"FAIL":"N/A"); row.put("heat_precondition",current.data().hasHeatData()?"fixture near threshold; native shots cross it":"N/A"); row.put("melee",nativeMelee?melees-meleeStart:"N/A: no default melee declared"); row.put("accepted_refit_views",views); row.put("status","PASS"); rows.add(row); System.out.println("[REVIEW native stock gun] "+row);
                if(++cursor==work.size()) { java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("bbstacz.review.gun")==null?"bbs-tacz-default-gameplay.json":"bbs-tacz-filtered-gameplay.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows)); System.out.println("[REVIEW gameplay complete] pairs="+rows.size()); return true; }
                begin(client,tick);
            }
        }
        return false;
    }
}
