package mcaxolotl.bbstacz;

import mcaxolotl.bbstacz.recording.WeakIdentityMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeakIdentityMapTest {
    private record EqualId(int id) {}
    @Test void equalClientAndServerIdsDoNotShareOwnership() { var map=new WeakIdentityMap<EqualId,String>(); var client=new EqualId(1); var server=new EqualId(1); map.put(client,"client"); map.put(server,"server"); assertEquals("client",map.get(client)); assertEquals("server",map.get(server)); assertEquals(2,map.keys().size()); }
    @Test void removalAndUpdatesAffectOnlyTheSameObject() { var map=new WeakIdentityMap<EqualId,String>(); var a=new EqualId(1); var b=new EqualId(1); map.put(a,"a"); map.put(b,"b"); map.put(a,"a2"); map.remove(b); assertEquals("a2",map.get(a)); assertNull(map.get(b)); assertFalse(map.isEmpty()); }
    @Test void computeDoesNotReuseAnEqualButDistinctKey() { var map=new WeakIdentityMap<EqualId,Object>(); var a=new EqualId(1); var b=new EqualId(1); var first=map.computeIfAbsent(a,k->new Object()); assertSame(first,map.computeIfAbsent(a,k->new Object())); assertNotSame(first,map.computeIfAbsent(b,k->new Object())); }
}
