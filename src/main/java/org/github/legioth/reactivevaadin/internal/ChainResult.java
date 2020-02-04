package org.github.legioth.reactivevaadin.internal;

import com.vaadin.flow.function.SerializableFunction;

public interface ChainResult<T> {
    T getValue();

    String getError();

    <C> ChainResult<C> mapValue(SerializableFunction<T, ChainResult<C>> mapper);

    static <T> ChainResult<T> value(T value) {
        return new ChainResult<T>() {
            @Override
            public T getValue() {
                return value;
            }

            @Override
            public String getError() {
                return null;
            }

            @Override
            public <C> ChainResult<C> mapValue(SerializableFunction<T, ChainResult<C>> mapper) {
                return mapper.apply(value);
            }
        };
    }

    static <T> ChainResult<T> error(String errorMessage) {
        return new ChainResult<T>() {
            @Override
            public T getValue() {
                return null;
            }

            @Override
            public String getError() {
                return errorMessage;
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public <C> ChainResult<C> mapValue(SerializableFunction<T, ChainResult<C>> mapper) {
                return (ChainResult) this;
            }
        };
    }
}
