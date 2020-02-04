package org.github.legioth.reactivevaadin.internal;

import org.github.legioth.reactivevaadin.Property;

@FunctionalInterface
public interface Combiner<T> {
    public interface Usage {
        <T> T valueOf(Property<T> property);

        default boolean flagValue(Property<Boolean> property) {
            return Boolean.TRUE.equals(valueOf(property));
        }
    }

    public interface BooleanCombiner {
        boolean combine(Usage using);
    }

    T combine(Usage using);
}