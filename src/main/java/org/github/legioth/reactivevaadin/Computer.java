package org.github.legioth.reactivevaadin;

import java.util.stream.Stream;

@FunctionalInterface
public interface Computer<T> {
    public interface Usage {
        <T> T valueOf(Property<T> property);

        default <T> Stream<T> valueOf(StreamProperty<T> properties) {
            return valueOf(properties.getStreamProperty()).get();
        }

        default <T> Iterable<T> iterableOf(StreamProperty<T> properties) {
            return () -> valueOf(properties.getStreamProperty()).get().iterator();
        }

        default boolean flagValue(Property<Boolean> property) {
            return Boolean.TRUE.equals(valueOf(property));
        }
    }

    public interface BooleanCombiner {
        boolean combine(Usage using);
    }

    T combine(Usage using);
}