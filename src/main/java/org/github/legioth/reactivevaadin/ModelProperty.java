package org.github.legioth.reactivevaadin;

import java.util.ArrayList;
import java.util.Objects;

import org.github.legioth.reactivevaadin.internal.CallbackSubscription;
import org.github.legioth.reactivevaadin.internal.Subscription;

import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.Command;

public class ModelProperty<T> implements Property<T> {

    private T value;

    private ArrayList<Command> listeners = new ArrayList<>();

    public ModelProperty(T initialValue) {
        value = initialValue;
    }

    public ModelProperty() {
        // nop
    }

    @Override
    public Subscription<T> subscribe() {
        return new CallbackSubscription<>(() -> value, listeners);
    }

    public void setValue(T value) {
        if (!Objects.equals(value, this.value)) {
            this.value = value;
            new ArrayList<>(listeners).forEach(Command::execute);
        }
    }

    public void update(SerializableFunction<T, T> updater) {
        setValue(updater.apply(value));
    }
}
