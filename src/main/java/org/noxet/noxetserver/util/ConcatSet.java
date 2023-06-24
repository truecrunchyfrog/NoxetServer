package org.noxet.noxetserver.util;

import java.util.*;

public class ConcatSet<E> implements Set<E> {
    private final List<Set<E>> concatenatedSets;

    @SafeVarargs
    public ConcatSet(Set<E>... sets) {
        concatenatedSets = new ArrayList<>();
        concatenatedSets.addAll(Arrays.asList(sets));
    }

    private Set<E> getSumSet() {
        Set<E> sumSet = new HashSet<>();

        for(Set<E> set : concatenatedSets)
            sumSet.addAll(set);

        return sumSet;
    }

    @Override
    public int size() {
        return getSumSet().size();
    }

    @Override
    public boolean isEmpty() {
        return getSumSet().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return getSumSet().contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return getSumSet().iterator();
    }

    @Override
    public Object[] toArray() {
        return getSumSet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return getSumSet().toArray(a);
    }

    @Override
    @Deprecated
    public boolean add(E e) {
        throw new Error("Cannot add to a concatenated set.");
    }

    @Override
    public boolean remove(Object o) {
        boolean changed = false;
        for(Set<E> set : concatenatedSets)
            if(set.remove(o) && !changed)
                changed = true;
        return changed;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return getSumSet().containsAll(c);
    }

    @Override
    @Deprecated
    public boolean addAll(Collection<? extends E> c) {
        throw new Error("Cannot add to a concatenated set.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        for(Set<E> set : concatenatedSets)
            if(set.retainAll(c) && !changed)
                changed = true;
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for(Set<E> set : concatenatedSets)
            if(set.removeAll(c) && !changed)
                changed = true;
        return changed;
    }

    @Override
    public void clear() {
        for(Set<E> set : concatenatedSets)
            set.clear();
    }
}
