package org.github.legioth.reactivevaadin;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.RandomAccess;
import java.util.stream.Stream;

import org.github.legioth.reactivevaadin.internal.CallbackSubscription;

import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.Command;

public class ModelList<T> extends AbstractList<T> implements StreamProperty<T>, RandomAccess {

    private final ArrayList<T> values = new ArrayList<>();

    private final ArrayList<Command> listeners = new ArrayList<>();

    private final Property<SerializableSupplier<Stream<T>>> streamProperty = () -> new CallbackSubscription<>(
            () -> this::stream, listeners);

    public ModelList() {
        super();
    }

    public ModelList(Collection<T> items) {
        this();
        addAll(items);
    }

    /* Basic List functionality */
    @Override
    public T get(int index) {
        return values.get(index);
    }

    @Override
    public int size() {
        return values.size();
    }

    /* Structural modifications that fire events */
    @Override
    public T set(int index, T element) {
        T old = values.set(index, element);
        fireEvent();
        return old;
    }

    @Override
    public void add(int index, T element) {
        values.add(index, element);
        fireEvent();
    }

    @Override
    public T remove(int index) {
        T oldValue = get(index);
        values.remove(index);
        fireEvent();
        return oldValue;
    }

    private void fireEvent() {
        new ArrayList<>(listeners).forEach(Command::execute);
    }

    /* Optimizations */
    @Override
    public void clear() {
        if (!isEmpty()) {
            values.clear();
            fireEvent();
        }
    }

    // TODO implement addAll and removeAll to avoid firing individual events

    @Override
    public Property<SerializableSupplier<Stream<T>>> getStreamProperty() {
        return streamProperty;
    }
}
