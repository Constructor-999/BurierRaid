package ch.jdtt.BurierRaid;

import java.util.ArrayList;
import java.util.Collection;

public class WarObservableArrayList<E> extends ArrayList<E> {
    private WarChangeListener warChangeListener;
    private String warHash;

    public WarObservableArrayList(WarChangeListener warChangeListener, String warHash) {
        super();
        this.warChangeListener = warChangeListener;
        this.warHash = warHash;
    }

    public WarObservableArrayList(Collection<? extends E> c, WarChangeListener warChangeListener, String warHash) {
        super(c);
        this.warChangeListener = warChangeListener;
        this.warHash = warHash;
    }

    public WarObservableArrayList(int initialCapacity, WarChangeListener warChangeListener, String warHash) {
        super(initialCapacity);
        this.warChangeListener = warChangeListener;
        this.warHash = warHash;
    }

    private void notifyChange() {
        if (warChangeListener != null) {
            warChangeListener.onChange(warHash);
        }
    }

    @Override
    public boolean add(E e) {
        boolean result = super.add(e);
        notifyChange();
        return result;
    }

    @Override
    public void add(int index, E element) {
        super.add(index, element);
        notifyChange();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean result = super.addAll(c);
        notifyChange();
        return result;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        boolean result = super.addAll(index, c);
        notifyChange();
        return result;
    }

    @Override
    public void clear() {
        super.clear();
        notifyChange();
    }

    @Override
    public E remove(int index) {
        E result = super.remove(index);
        notifyChange();
        return result;
    }

    @Override
    public boolean remove(Object o) {
        boolean result = super.remove(o);
        notifyChange();
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = super.removeAll(c);
        notifyChange();
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean result = super.retainAll(c);
        notifyChange();
        return result;
    }

    @Override
    public E set(int index, E element) {
        E result = super.set(index, element);
        notifyChange();
        return result;
    }
}
