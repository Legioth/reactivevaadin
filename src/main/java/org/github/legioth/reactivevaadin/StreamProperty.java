package org.github.legioth.reactivevaadin;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.github.legioth.reactivevaadin.internal.CombineSubscription;

import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.SerializableSupplier;

@FunctionalInterface
public interface StreamProperty<T> {

    Property<SerializableSupplier<Stream<T>>> getStreamProperty();

    /* Fundamental compositions */
    default <U> StreamProperty<U> mapProperties(SerializableFunction<T, Property<U>> mapper) {
        /*
         * Must eagerly collect the stream so that using::valueOf happens before
         * the combiner returns.
         * 
         * TODO: How bad is this for performance?
         */
        return fromProperty(CombineSubscription.combine(using -> {
            Stream<T> baseStream = using.valueOf(getStreamProperty()).get();
            List<U> propertyValues = baseStream.map(mapper).map(using::valueOf).collect(Collectors.toList());
            return propertyValues::stream;
        }));
    }

    default <U> StreamProperty<U> mapStream(SerializableFunction<Stream<T>, Stream<U>> mapper) {
        return fromProperty(getStreamProperty().map(streamSupplier -> () -> mapper.apply(streamSupplier.get())));
    }

    default <U> Property<U> terminateStream(SerializableFunction<Stream<T>, U> mapper) {
        return getStreamProperty().map(streamSupplier -> mapper.apply(streamSupplier.get()));
    }

    /* Derived StreamProperty compositions */
    default <U> StreamProperty<U> map(SerializableFunction<T, U> mapper) {
        return mapStream(stream -> stream.map(mapper));
    }

    default StreamProperty<T> filter(SerializablePredicate<T> filter) {
        return mapStream(stream -> stream.filter(filter));
    }

    /* Derived Property compositions */
    default <U> Property<U> collect(Collector<T, ?, U> collector) {
        return terminateStream(stream -> stream.collect(collector));
    }

    default Flag anyMatch(SerializablePredicate<T> predicate) {
        return Flag.fromProperty(terminateStream(stream -> Boolean.valueOf(stream.anyMatch(predicate))));
    }

    default Flag allMatch(SerializablePredicate<T> predicate) {
        return Flag.fromProperty(terminateStream(stream -> Boolean.valueOf(stream.allMatch(predicate))));
    }

    default Property<Long> count() {
        return terminateStream(Stream::count);
    }

    default Property<T> reduce(T identity, BinaryOperator<T> accumulator) {
        return terminateStream(stream -> stream.reduce(identity, accumulator));
    }

    /* Wrapper */
    static <T> StreamProperty<T> fromProperty(Property<SerializableSupplier<Stream<T>>> property) {
        return () -> property;
    }
}
