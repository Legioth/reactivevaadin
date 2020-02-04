package org.github.legioth.reactivevaadin;

import org.github.legioth.reactivevaadin.internal.CombineSubscription;

public interface Flag extends Property<Boolean> {

    default Flag and(Property<Boolean> other) {
        return CombineSubscription.combineFlag(using -> using.flagValue(this) && using.flagValue(other));
    }

    default Flag or(Property<Boolean> other) {
        return CombineSubscription.combineFlag(using -> using.flagValue(this) || using.flagValue(other));
    }

    default Flag not() {
        return () -> subscribe().map(value -> Boolean.valueOf(!Boolean.TRUE.equals(value)));
    }

    public static Flag fromProperty(Property<Boolean> property) {
        return () -> property.subscribe();
    }

    default <T> Property<T> branch(T trueValue, T falseValue) {
        return map(value -> Boolean.TRUE.equals(value) ? trueValue : falseValue);
    }

    default <T> Property<T> branch(Property<T> trueValue, T falseValue) {
        return CombineSubscription.combine(using -> using.flagValue(this) ? using.valueOf(trueValue) : falseValue);
    }

    default <T> Property<T> branch(T trueValue, Property<T> falseValue) {
        return CombineSubscription.combine(using -> using.flagValue(this) ? trueValue : using.valueOf(falseValue));
    }

    default <T> Property<T> branch(Property<T> trueValue, Property<T> falseValue) {
        return CombineSubscription
                .combine(using -> using.flagValue(this) ? using.valueOf(trueValue) : using.valueOf(falseValue));
    }
}
