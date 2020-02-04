package org.github.legioth.reactivevaadin.internal;

import java.util.ArrayList;

import org.github.legioth.reactivevaadin.Property;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

public abstract class PropertyWrapper<U, D> implements Property<D> {
    private static class PropertyWrapperSubscription<D> implements Subscription<D> {
        private final PropertyWrapper<?, ?> property;
        private final SerializableSupplier<D> valueSupplier;

        private boolean attached = false;
        private Command invalidateCommand;

        public PropertyWrapperSubscription(PropertyWrapper<?, ?> property, SerializableSupplier<D> valueSupplier) {
            this.property = property;
            this.valueSupplier = valueSupplier;
        }

        @Override
        public Registration enable(SerializableConsumer<Subscription<D>> subscriber, SubscriptionContext context) {
            invalidateCommand = () -> subscriber.accept(this);

            property.addDownstreamSubscription(invalidateCommand);
            Registration contextRegistration = addContext(context);

            return () -> {
                contextRegistration.remove();

                property.removeDownstreamSubscription(invalidateCommand);
                invalidateCommand = null;
            };
        }

        private Registration addContext(SubscriptionContext context) {
            return context.addAttachListener(ui -> {
                attach(ui);
                invalidateCommand.execute();
                return () -> detach();
            });
        }

        private void detach() {
            if (attached) {
                attached = false;

                property.decreaseAttachCount();
            }
        }

        private void attach(UI ui) {
            if (!attached) {
                attached = true;

                property.increaseAttachCount(ui);
            }
        }

        @Override
        public D getValue() {
            return valueSupplier.get();
        }

        @Override
        public <T> Subscription<T> map(SerializableFunction<D, T> mapper) {
            return new PropertyWrapperSubscription<>(property, () -> mapper.apply(valueSupplier.get()));
        }
    }

    private final Property<U> upstreamProperty;

    private Subscription<U> upstreamSubscription;
    private Registration subscriptionRegistration;

    private ArrayList<Command> listeners = new ArrayList<>();

    private ArrayList<SerializableFunction<UI, Registration>> attachListeners = new ArrayList<>();
    private ArrayList<Registration> detachListeners = new ArrayList<>();

    private int attachCount = 0;
    private UI attachedUi = null;

    private final SubscriptionContext proxyContext = attachListener -> {
        attachListeners.add(attachListener);
        if (attachedUi != null) {
            detachListeners.add(attachListener.apply(attachedUi));
        }
        // TODO Should maybe trigger the corresponding detach listener if one
        // exists?
        return () -> attachListeners.remove(attachListener);
    };

    private D value = Util.missingValueToken();

    private Registration valueChangeRegistration;

    public PropertyWrapper(Property<U> upstreamProperty) {
        this.upstreamProperty = upstreamProperty;
    }

    abstract protected D getInitialValue(U upstreamValue);

    abstract protected Registration onUpstreamValueChange(U newUpstreamValue, SerializableConsumer<D> downstreamValueUpdater);

    protected UI getAttachedUi() {
        return attachedUi;
    }

    private void invalidate() {
        valueChangeRegistration = onUpstreamValueChange(getUpstreamValue(), downstreamValue -> {
            valueChangeRegistration = null;
            value = downstreamValue;
            new ArrayList<>(listeners).forEach(Command::execute);
        });
    }

    private D getDownstreamValue() {
        if (value == Util.missingValueToken()) {
            value = getInitialValue(getUpstreamValue());
        }

        return value;
    }

    private U getUpstreamValue() {
        return getSubscription().getValue();
    }

    private Subscription<U> getSubscription() {
        if (upstreamSubscription == null) {
            upstreamSubscription = upstreamProperty.subscribe();
        }
        return upstreamSubscription;
    }

    private void enableSubscription() {
        subscriptionRegistration = getSubscription().enable(ignore -> invalidate(), proxyContext);
    }

    private void addDownstreamSubscription(Command command) {
        if (listeners.isEmpty()) {
            enableSubscription();
        }
        listeners.add(command);
    }

    private void removeDownstreamSubscription(Command command) {
        listeners.remove(command);
        if (listeners.isEmpty()) {
            subscriptionRegistration.remove();
            subscriptionRegistration = null;
            upstreamSubscription = null;
        }
    }

    private void increaseAttachCount(UI ui) {
        attachCount++;
        if (attachedUi == null) {
            attachedUi = ui;

            attachListeners.forEach(listener -> detachListeners.add(listener.apply(ui)));
        } else if (ui != attachedUi) {
            throw new IllegalStateException();
        }
    }

    private void decreaseAttachCount() {
        attachCount--;
        if (attachCount == 0) {
            ArrayList<Registration> detachers = new ArrayList<>(detachListeners);
            detachListeners.clear();
            detachers.forEach(Registration::remove);

            if (valueChangeRegistration != null) {
                valueChangeRegistration.remove();
                valueChangeRegistration = null;
            }

            attachedUi = null;
        }
    }

    @Override
    public Subscription<D> subscribe() {
        return new PropertyWrapperSubscription<>(this, this::getDownstreamValue);
    }
}
