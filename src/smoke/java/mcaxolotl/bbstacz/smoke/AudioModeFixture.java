package mcaxolotl.bbstacz.smoke;

import mcaxolotl.bbstacz.forms.TaczForm;
import mcaxolotl.bbstacz.recording.FrameSnapshot;
import mcaxolotl.bbstacz.recording.NativeSoundTimeline;
import mcaxolotl.bbstacz.recording.TaczReplayChannels;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import net.minecraft.client.Minecraft;

final class AudioModeFixture {
    static Film moving(Minecraft client,boolean fp) {
        return moving(client,fp,true);
    }
    static Film moving(Minecraft client,boolean fp,boolean tracking) {
        Film film=new Film(); film.setId("bbs-tacz-audio-mode-"+System.currentTimeMillis());
        var camera=new mchorse.bbs_mod.camera.clips.overwrite.IdleClip(); camera.duration.set(180); film.camera.addClip(camera);
        Replay replay=new Replay("moving_sound_actor"); replay.form.set(new TaczForm()); replay.fp.set(fp); film.replays.add(replay);
        replay.keyframes.x.insert(0,client.player.getX()); replay.keyframes.x.insert(120,client.player.getX()+8);
        replay.keyframes.y.insert(0,client.player.getY()); replay.keyframes.z.insert(0,client.player.getZ());
        var channels=(TaczReplayChannels)replay.keyframes; channels.bbstacz$state().insert(0,FrameSnapshot.EMPTY); channels.bbstacz$state().insert(160,FrameSnapshot.EMPTY);
        for(int tick:new int[]{0,20,40,60,80,100}) NativeSoundTimeline.append(channels.bbstacz$sounds(),tick,
            NativeSoundTimeline.play("native-"+tick,"tacz:ak47/ak47_reload_mvt",0.8f,1,100,false,0,tracking,true,30));
        return film;
    }
}
