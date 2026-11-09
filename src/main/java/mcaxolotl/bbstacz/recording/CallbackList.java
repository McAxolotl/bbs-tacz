package mcaxolotl.bbstacz.recording;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * A small lifecycle registry whose iteration may invoke callbacks that change the registry.
 * Snapshot entries have identity tokens: removing an old entry never removes its replacement,
 * even when the values compare equal. Iterator removal is supported (unlike a raw COW list).
 */
public final class CallbackList<E> extends AbstractList<E> {
    private static final class Entry<E> {
        final E value;
        Entry(E value) { this.value = value; }
    }
    private final CopyOnWriteArrayList<Entry<E>> entries = new CopyOnWriteArrayList<>();

    public CallbackList(Collection<? extends E> values) { addAll(values); }
    @Override public int size() { return entries.size(); }
    @Override public E get(int index) { return entries.get(index).value; }
    @Override public E set(int index, E value) { return entries.set(index, new Entry<>(value)).value; }
    @Override public boolean add(E value) { entries.add(new Entry<>(value)); return true; }
    @Override public void add(int index, E value) { entries.add(index, new Entry<>(value)); }
    @Override public E remove(int index) { return entries.remove(index).value; }
    @Override public boolean remove(Object value) {
        for (Entry<E> entry : entries) if (Objects.equals(value, entry.value) && entries.remove(entry)) return true;
        return false;
    }
    @Override public void clear() { entries.clear(); }
    @Override public Iterator<E> iterator() {
        var snapshot = entries.iterator();
        return new Iterator<>() {
            private Entry<E> last;
            private boolean removable;
            @Override public boolean hasNext() { return snapshot.hasNext(); }
            @Override public E next() { last = snapshot.next(); removable = true; return last.value; }
            @Override public void remove() {
                if (!removable) throw new IllegalStateException("No unremoved entry from next()");
                entries.remove(last); removable = false;
            }
        };
    }
    @Override public boolean removeIf(Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate);
        boolean changed = false;
        for (Entry<E> entry : entries) {
            // A prior callback may have stopped this player, or replaced the entire registry.
            if (entries.contains(entry) && predicate.test(entry.value)) changed |= entries.remove(entry);
        }
        return changed;
    }
}
