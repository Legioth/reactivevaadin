package org.github.legioth.reactivevaadin.internal;

public interface FieldBindingChain<P, F> {

    ChainResult<P> toProperty(ChainResult<F> fieldValue);

    ChainResult<F> toField(ChainResult<P> propertyValue);

    static <T> FieldBindingChain<T, T> empty() {
        return new FieldBindingChain<T, T>() {
            @Override
            public ChainResult<T> toProperty(ChainResult<T> fieldValue) {
                return fieldValue;
            }
    
            @Override
            public ChainResult<T> toField(ChainResult<T> propertyValue) {
                return propertyValue;
            }
        };
    }

}
