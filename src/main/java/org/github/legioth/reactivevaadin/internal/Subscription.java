package org.github.legioth.reactivevaadin.internal;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;

public interface Subscription<T> {
    Registration enable(SerializableConsumer<Subscription<T>> subscriber, SubscriptionContext context);

    T getValue();

    <U> Subscription<U> map(SerializableFunction<T, U> mapper);
}