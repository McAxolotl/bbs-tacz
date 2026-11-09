package mcaxolotl.bbstacz.recording;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

/** Weak object-identity keys. Minecraft Entity.equals compares ids shared by client/server peers. Caller owns synchronization. */
public final class WeakIdentityMap<K, V> {
    private static final class Key<K> extends WeakReference<K> {
        private final int hash;
        Key(K value, ReferenceQueue<K> queue) { super(java.util.Objects.requireNonNull(value), queue); hash=System.identityHashCode(value); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) { return this==other || other instanceof Key<?> key && get()!=null && get()==key.get(); }
    }
    private final ReferenceQueue<K> queue=new ReferenceQueue<>();
    private final HashMap<Key<K>,V> entries=new HashMap<>();
    private void reap() { for(var key=queue.poll();key!=null;key=queue.poll()) entries.remove(key); }
    public V get(K key) { reap(); return entries.get(new Key<>(key,null)); }
    public void put(K key,V value) { reap(); entries.put(new Key<>(key,queue),value); }
    public V remove(K key) { reap(); return entries.remove(new Key<>(key,null)); }
    public V computeIfAbsent(K key,Function<K,V> factory) { V value=get(key); if(value==null) { value=factory.apply(key); if(value!=null) put(key,value); } return value; }
    public boolean isEmpty() { reap(); return entries.isEmpty(); }
    public List<V> values() { reap(); return List.copyOf(entries.values()); }
    public List<K> keys() { reap(); var keys=new ArrayList<K>(); for(var key:entries.keySet()) { K value=key.get(); if(value!=null) keys.add(value); } return keys; }
}
