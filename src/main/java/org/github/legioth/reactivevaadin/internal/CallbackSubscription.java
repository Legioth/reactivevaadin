package org.github.legioth.reactivevaadin.internal;

import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

public class CallbackSubscription<T> implements Subscription<T> {

    private final SerializableSupplier<T> valueSupplier;
    private final SerializableBiFunction<UI, Command, Registration> activationHandler;

    private T value = Util.missingValueToken();

    public CallbackSubscription(SerializableSupplier<T> valueSupplier, List<Command> listeners) {
        this(valueSupplier, (ui, command) -> {
            listeners.add(command);
            return () -> listeners.remove(command);
        });
    }

    public CallbackSubscription(SerializableSupplier<T> valueSupplier,
            SerializableFunction<Command, Registration> activationHandler) {
        this(valueSupplier, (ui, command) -> activationHandler.apply(command));
    }

    public CallbackSubscription(SerializableSupplier<T> valueSupplier,
            SerializableBiFunction<UI, Command, Registration> activationHandler) {
        this.valueSupplier = valueSupplier;
        this.activationHandler = activationHandler;
    }

    @Override
    public Registration enable(SerializableConsumer<Subscription<T>> subscriber, SubscriptionContext context) {
        return new Registration() {
            private final Registration contextRegistration = context.addAttachListener(ui -> {
                updateValue(true);
                return activationHandler.apply(ui, () -> updateValue(false));
            });

            private void updateValue(boolean forceNotify) {
                T newValue = valueSupplier.get();

                if (forceNotify || !Objects.equals(value, newValue)) {
                    value = newValue;
                    subscriber.accept(CallbackSubscription.this);
                }
            }

            @Override
            public void remove() {
                contextRegistration.remove();
            }
        };
    }

    @Override
    public T getValue() {
        if (value == Util.missingValueToken()) {
            value = valueSupplier.get();
            assert value != Util.missingValueToken();
        }
        return value;
    }

    @Override
    public <U> Subscription<U> map(SerializableFunction<T, U> mapper) {
        Objects.requireNonNull(mapper);
        return new CallbackSubscription<>(() -> mapper.apply(valueSupplier.get()), activationHandler);
    }
}
