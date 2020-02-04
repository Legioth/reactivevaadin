package org.github.legioth.reactivevaadin.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.github.legioth.reactivevaadin.Computer;
import org.github.legioth.reactivevaadin.Flag;
import org.github.legioth.reactivevaadin.Property;
import org.github.legioth.reactivevaadin.Computer.BooleanCombiner;
import org.github.legioth.reactivevaadin.Computer.Usage;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.StateTree.ExecutionRegistration;
import com.vaadin.flow.shared.Registration;

/*
 * TODO Multiple subscriptions to the property based on a combine should only subscribe to
 * dependencies once since combining is somewhat expensive
 */

public class CombineSubscription<T> implements Subscription<T> {
    private class PropertySubscription {
        private final Subscription<?> subscription;

        private Registration registration;

        public PropertySubscription(Subscription<?> subscription) {
            this.subscription = subscription;
        }

        public void enableSubscription() {
            if (registration != null) {
                throw new IllegalStateException();
            }

            registration = subscription.enable(ignore -> invalidate(), context);
        }

        public void unregister() {
            if (registration != null) {
                registration = null;
            }
        }
    }

    private final Computer<T> combiner;

    private final HashSet<Property<?>> lastUsed = new HashSet<>();
    private Map<Property<?>, PropertySubscription> properties = new HashMap<>();

    private T value = Util.missingValueToken();

    // Set when enabled
    private SubscriptionContext context;
    private SerializableConsumer<Subscription<T>> subscriber;

    // Set when attached
    private UI ui;

    private boolean pendingCleanup;

    private ExecutionRegistration cleanupRegistration;
    private ExecutionRegistration flushRegistration;

    public CombineSubscription(Computer<T> combiner) {
        this.combiner = combiner;
    }

    public <U, V> CombineSubscription(Property<U> first, Property<V> second, SerializableBiFunction<U, V, T> combiner) {
        this(using -> combiner.apply(using.valueOf(first), using.valueOf(second)));
    }

    @Override
    public Registration enable(SerializableConsumer<Subscription<T>> subscriber, SubscriptionContext context) {
        if (subscriber == null) {
            throw new IllegalStateException();
        }
        this.subscriber = subscriber;
        this.context = context;

        properties.values().forEach(PropertySubscription::enableSubscription);
        Registration attachRegistration = context.addAttachListener(ui -> {
            this.ui = ui;

            scheduleFlush();

            if (pendingCleanup) {
                scheduleCleanup();
            }

            return () -> {
                if (cleanupRegistration != null) {
                    cleanupRegistration.remove();
                    cleanupRegistration = null;
                }
                if (flushRegistration != null) {
                    flushRegistration.remove();
                    flushRegistration = null;
                }

                this.ui = null;
            };
        });

        return () -> {
            attachRegistration.remove();
            properties.values().forEach(status -> status.unregister());
        };
    }

    private void invalidate() {
        value = Util.missingValueToken();

        if (ui != null) {
            scheduleFlush();
        }
    }

    private void scheduleFlush() {
        if (flushRegistration != null) {
            return;
        }
        flushRegistration = ui.beforeClientResponse(ui, ignore -> {
            flushRegistration = null;

            subscriber.accept(this);
        });
    }

    private T computeValue() {
        lastUsed.clear();

        boolean[] combinerReturned = { false };
        T computed = combiner.combine(new Usage() {
            @Override
            public <S> S valueOf(Property<S> property) {
                if (combinerReturned[0]) {
                    throw new IllegalStateException();
                }
                lastUsed.add(property);
                PropertySubscription status = properties.get(property);

                if (status == null) {
                    status = new PropertySubscription(property.subscribe());

                    properties.put(property, status);

                    // If enabled
                    if (context != null) {
                        status.enableSubscription();
                    }
                }

                @SuppressWarnings("unchecked")
                Subscription<S> subscription = (Subscription<S>) status.subscription;

                return subscription.getValue();
            }
        });
        combinerReturned[0] = true;

        if (lastUsed.isEmpty()) {
            throw new IllegalStateException();
        }

        pendingCleanup = true;
        if (ui != null) {
            scheduleCleanup();
        }

        return computed;
    }

    private void scheduleCleanup() {
        if (cleanupRegistration != null) {
            // Already scheduled
            return;
        }
        cleanupRegistration = ui.beforeClientResponse(ui, ignore -> {
            cleanupRegistration = null;
            pendingCleanup = false;

            // TODO Avoid copying by using Iterator.remove()
            properties.keySet().stream().filter(property -> !lastUsed.contains(property)).collect(Collectors.toList())
                    .forEach(unused -> properties.remove(unused).unregister());
        });
    }

    @Override
    public T getValue() {
        if (value == Util.missingValueToken()) {
            value = computeValue();
        }
        return value;
    }

    @Override
    public <U> Subscription<U> map(SerializableFunction<T, U> mapper) {
        // Explicitly extract to avoid closing over this
        Computer<T> combiner = this.combiner;

        return new CombineSubscription<>(usage -> mapper.apply(combiner.combine(usage)));
    }

    public static <T> Property<T> combine(Computer<T> combiner) {
        /*
         * Dummy wrap to avoid creating multiple combine subscriptions if there
         * are multiple downstream subscriptions.
         * 
         * Could maybe use .beforeClientResponse() instead and then also remove
         * some logic from CombineSubscription?
         */
        return new PropertyWrapper<T, T>(() -> new CombineSubscription<>(combiner)) {
            @Override
            protected T getInitialValue(T upstreamValue) {
                return upstreamValue;
            }

            @Override
            protected Registration onUpstreamValueChange(T newUpstreamValue,
                    SerializableConsumer<T> downstreamValueUpdater) {
                downstreamValueUpdater.accept(newUpstreamValue);
                return null;
            }
        };
    }

    public static Flag combineFlag(BooleanCombiner combiner) {
        return Flag.fromProperty(combine(usage -> Boolean.valueOf(combiner.combine(usage))));
    }
}
