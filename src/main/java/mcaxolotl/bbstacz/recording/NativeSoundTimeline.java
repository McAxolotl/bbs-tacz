package mcaxolotl.bbstacz.recording;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

/** Serialized native sound commands. Contains resource ids/parameters only, never audio assets or BBS audio clips. */
public final class NativeSoundTimeline {
    public record Cue(String id,String name,float tick,float end,float volume,float pitch,int distance,
                      boolean mono,int limit,boolean tracking,boolean relative) {}
    private NativeSoundTimeline() {}
    public static long revision(KeyframeChannel<String> channel) {
        long hash=0xcbf29ce484222325L;
        for(var key:channel.getKeyframes()) {
            hash=(hash^Float.floatToIntBits(key.getTick()))*0x100000001b3L;
            String text=key.getValue(); for(int i=0;i<text.length();i++) hash=(hash^text.charAt(i))*0x100000001b3L;
        }
        return hash;
    }
    public static List<Cue> read(KeyframeChannel<String> channel) {
        var plays=new ArrayList<Cue>(); var stops=new HashMap<String,Float>();
        for (var key:channel.getKeyframes()) {
            try {
                if (key.getValue().length()>1_048_576) continue;
                var batch=JsonParser.parseString(key.getValue()).getAsJsonArray();
                for (var element:batch) {
                    var event=element.getAsJsonObject();
                    if (event.get("v").getAsInt()!=1) continue;
                    String id=event.get("id").getAsString(); if (id.length()>128) continue;
                    if (event.get("op").getAsString().equals("stop")) { stops.merge(id,key.getTick(),Math::min); continue; }
                    if (!event.get("op").getAsString().equals("play")) continue;
                    String name=event.get("name").getAsString();
                    float volume=event.get("volume").getAsFloat(), pitch=event.get("pitch").getAsFloat();
                    float duration=event.get("duration").getAsFloat();
                    if (name.length()>512 || !name.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") || !Float.isFinite(volume)
                        || !Float.isFinite(pitch) || !Float.isFinite(duration) || duration<=0 || pitch<=0 || volume<=0) continue;
                    plays.add(new Cue(id,name,key.getTick(),key.getTick()+duration,Math.min(volume,16),Math.max(0.5f,Math.min(2,pitch)),
                        Math.max(1,Math.min(1024,event.get("distance").getAsInt())),event.get("mono").getAsBoolean(),
                        Math.max(0,Math.min(128,event.get("limit").getAsInt())),event.get("tracking").getAsBoolean(),event.get("relative").getAsBoolean()));
                }
            } catch (RuntimeException invalid) { /* One corrupt/unknown batch must not prevent other replay channels playing. */ }
        }
        var result=new ArrayList<Cue>();
        for (var cue:plays) result.add(new Cue(cue.id,cue.name,cue.tick,Math.min(cue.end,stops.getOrDefault(cue.id,Float.POSITIVE_INFINITY)),
            cue.volume,cue.pitch,cue.distance,cue.mono,cue.limit,cue.tracking,cue.relative));
        result.sort(Comparator.comparingDouble(Cue::tick)); return List.copyOf(result);
    }
    public static JsonObject play(String id,String name,float volume,float pitch,int distance,boolean mono,int limit,boolean tracking,boolean relative,float duration) {
        var event=base(id,"play"); event.addProperty("name",name); event.addProperty("volume",volume); event.addProperty("pitch",pitch);
        event.addProperty("distance",distance); event.addProperty("mono",mono); event.addProperty("limit",limit);
        event.addProperty("tracking",tracking); event.addProperty("relative",relative); event.addProperty("duration",duration); return event;
    }
    public static JsonObject stop(String id) { return base(id,"stop"); }
    private static JsonObject base(String id,String operation) { var event=new JsonObject(); event.addProperty("v",1); event.addProperty("id",id); event.addProperty("op",operation); return event; }
    public static void append(KeyframeChannel<String> channel,float tick,JsonObject event) {
        JsonArray batch=new JsonArray();
        for (var key:channel.getKeyframes()) if (key.getTick()==tick) {
            try { batch=JsonParser.parseString(key.getValue()).getAsJsonArray(); } catch (RuntimeException invalid) {} break;
        }
        if (event!=null) batch.add(event); channel.insert(tick,batch.toString());
    }
}
