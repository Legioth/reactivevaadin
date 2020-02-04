package org.github.legioth.reactivevaadin;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.github.legioth.reactivevaadin.internal.CombineSubscription;
import org.github.legioth.reactivevaadin.internal.PropertyWrapper;
import org.github.legioth.reactivevaadin.internal.Subscription;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.shared.Registration;

@FunctionalInterface
public interface Property<T> {
    Subscription<T> subscribe();

    /* Fundamental compositions */
    default <U> Property<U> map(SerializableFunction<T, U> mapper) {
        return () -> subscribe().map(mapper);
    }

    default <U> Property<U> flatMap(SerializableFunction<T, Property<U>> mapper) {
        return CombineSubscription.combine(usage -> usage.valueOf(mapper.apply(usage.valueOf(this))));
    }

    default <U, R> Property<R> combine(Property<U> other, SerializableBiFunction<T, U, R> combiner) {
        return CombineSubscription.combine(using -> combiner.apply(using.valueOf(this), using.valueOf(other)));
    }

    default Property<T> filter(SerializablePredicate<T> filter) {
        return new PropertyWrapper<T, T>(this) {
            @Override
            protected T getInitialValue(T upstreamValue) {
                return filter.test(upstreamValue) ? upstreamValue : null;
            }

            @Override
            protected Registration onUpstreamValueChange(T upstreamValue,
                    SerializableConsumer<T> downstreamValueUpdater) {
                if (filter.test(upstreamValue)) {
                    downstreamValueUpdater.accept(upstreamValue);
                }
                return null;
            }
        };
    }

    /*
     * TODO Consider a lazily populated ModelList for cases when results are
     * slowly dropping in, or for the classical chat use case.
     * 
     * TODO Provide a way of resetting the downstream value while a new one is
     * computed
     */
    default <U> Property<U> mapAsync(U initialValue,
            SerializableBiFunction<T, SerializableConsumer<U>, Registration> scheduler) {
        return new PropertyWrapper<T, U>(this) {
            private Registration currentJob;

            @Override
            protected U getInitialValue(T upstreamValue) {
                return initialValue;
            }

            @Override
            protected Registration onUpstreamValueChange(T input, SerializableConsumer<U> downstreamValueUpdater) {
                // TODO Avoid starting a new job for the same value as before

                UI ui = getAttachedUi();

                // TODO Allow waiting and submitting latest instead of canceling
                // TODO Allow keeping x jobs in flight at the same time
                if (currentJob != null) {
                    currentJob.remove();
                }

                currentJob = scheduler.apply(input, result -> {
                    // TODO Ignore result if a newer job has completed (broken
                    // cancelation or if we allow multiple concurrent jobs)
                    ui.access(() -> {
                        currentJob = null;
                        downstreamValueUpdater.accept(result);
                    });
                });
                /*
                 * While we could cancel the job if all target components are
                 * detached, we don't want to risk canceling something just
                 * because a single target component is detached and attached
                 * during the same round trip.
                 * 
                 * TODO cancel in beforeClientResponse?
                 */
                return null;
            }
        };
    }

    default Property<T> beforeClientResponse() {
        return new PropertyWrapper<T, T>(this) {
            private T newUpstreamValue;
            private Registration pendingClientResponse;

            @Override
            protected T getInitialValue(T upstreamValue) {
                return upstreamValue;
            }

            @Override
            protected Registration onUpstreamValueChange(T newUpstreamValue,
                    SerializableConsumer<T> downstreamValueUpdater) {
                this.newUpstreamValue = newUpstreamValue;
                if (pendingClientResponse == null) {
                    UI ui = getAttachedUi();
                    pendingClientResponse = ui.beforeClientResponse(ui, ignore -> {
                        pendingClientResponse = null;
                        downstreamValueUpdater.accept(this.newUpstreamValue);
                        this.newUpstreamValue = null;
                    });
                }
                return pendingClientResponse;
            }
        };

    }

    /* Derived compositions */
    default <U> Property<U> mapOrNull(SerializableFunction<T, U> mapper) {
        return mapOrDefault(mapper, null);
    }

    default <U> Property<U> mapOrDefault(SerializableFunction<T, U> mapper, U defaultValue) {
        return map(value -> Optional.ofNullable(value).map(mapper).orElse(defaultValue));
    }

    default Property<T> orElseGet(SerializableSupplier<T> nullValueSupplier) {
        return map(value -> {
            if (value == null) {
                return nullValueSupplier.get();
            } else {
                return value;
            }
        });
    }

    default Property<T> orElse(T nullValue) {
        return orElseGet(() -> nullValue);
    }

    default Property<String> format(String formatString) {
        return map(value -> String.format(formatString, value));
    }

    default Flag hasValue() {
        return test(Objects::nonNull);
    }

    default Flag test(SerializablePredicate<T> predicate) {
        return Flag.fromProperty(map(value -> Boolean.valueOf(predicate.test(value))));
    }

    default Flag isEqual(Property<T> other) {
        return Flag.fromProperty(combine(other, Objects::equals));
    }

    default <U> Property<U> mapAsync(U initialValue, SerializableFunction<T, CompletableFuture<U>> mapper) {
        return mapAsync(initialValue, (T input, SerializableConsumer<U> consumer) -> {
            CompletableFuture<U> future = mapper.apply(input);
            future.thenAccept(consumer);
            return () -> future.cancel(true);
        });
    }

    static <U> Property<U> computed(Computer<U> computer) {
        return CombineSubscription.combine(computer);
    }
}
