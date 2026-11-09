package mcaxolotl.bbstacz.recording;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseValue;

/** Opaque, namespaced action data remains a BBS map and survives copying/editing a clip. */
public final class ValuePayload extends BaseValue {
    private MapType value = new MapType();
    public ValuePayload(String id) { super(id); }
    public MapType get() { return value; }
    public void set(MapType map) { value = (MapType) map.copy(); }
    @Override public BaseType toData() { return value.copy(); }
    @Override public void fromData(BaseType data) {
        if (!(data instanceof MapType map)) throw new IllegalArgumentException("BBS TACZ payload must be a map");
        set(map);
    }
    @Override public void copy(BaseValue other) { if (other instanceof ValuePayload payload) set(payload.value); }
    @Override public void reset() { value = new MapType(); }
}
