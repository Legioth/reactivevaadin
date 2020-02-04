package org.github.legioth.reactivevaadin.internal;

import org.github.legioth.reactivevaadin.FieldBinding;
import org.github.legioth.reactivevaadin.FieldBindingBuilder;
import org.github.legioth.reactivevaadin.ModelProperty;
import org.github.legioth.reactivevaadin.Property;

import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializablePredicate;

public class FieldBindingBuilderImpl<P, F> implements FieldBindingBuilder<P, F> {

    private final HasValueAndElement<?, F> field;
    private final SerializableConsumer<FieldBinding<?, F>> doneHandler;
    private FieldBindingChain<P, F> chain;

    public FieldBindingBuilderImpl(HasValueAndElement<?, F> field, FieldBindingChain<P, F> chain,
            SerializableConsumer<FieldBinding<?, F>> doneHandler) {
        this.field = field;
        this.chain = chain;
        this.doneHandler = doneHandler;
    }

    @Override
    public <C> FieldBindingBuilder<C, F> withConverter(SerializableFunction<P, C> toModel,
            SerializableFunction<C, P> toField, String errorMessage) {
        return new FieldBindingBuilderImpl<>(field, new FieldBindingChain<C, F>() {
            @Override
            public ChainResult<C> toProperty(ChainResult<F> fieldValue) {
                return chain.toProperty(fieldValue).mapValue(value -> {
                    try {
                        return ChainResult.value(toModel.apply(value));
                    } catch (RuntimeException e) {
                        return ChainResult.error(errorMessage == null ? e.getMessage() : errorMessage);
                    }
                });
            }

            @Override
            public ChainResult<F> toField(ChainResult<C> propertyValue) {
                return chain.toField(propertyValue.mapValue(value -> ChainResult.value(toField.apply(value))));
            }
        }, doneHandler);
    }

    @Override
    public FieldBindingBuilder<P, F> withValidator(SerializablePredicate<P> test, String errorMessage) {
        return new FieldBindingBuilderImpl<>(field, new FieldBindingChain<P, F>() {
            @Override
            public ChainResult<P> toProperty(ChainResult<F> fieldValue) {
                ChainResult<P> propertyValue = chain.toProperty(fieldValue);
                if (propertyValue.getError() == null && !test.test(propertyValue.getValue())) {
                    return ChainResult.error(errorMessage);
                } else {
                    return propertyValue;
                }
            }

            @Override
            public ChainResult<F> toField(ChainResult<P> propertyValue) {
                return chain.toField(propertyValue);
            }
        }, doneHandler);
    }

    @Override
    public FieldBinding<P, F> bind(ModelProperty<P> property) {
        FieldBinding<P, F> binding = new FieldBinding<>(property, chain, field);
        doneHandler.accept(binding);
        return binding;
    }

    @Override
    public FieldBindingBuilder<P, F> asRequired(String requiredMessage) {
        return new FieldBindingBuilderImpl<>(field, chain, binding -> {
            binding.setRequired(requiredMessage);
            doneHandler.accept(binding);
        });
    }

    @Override
    public FieldBindingBuilder<P, F> asRequired(Property<Boolean> required, String errorMessage) {
        return new FieldBindingBuilderImpl<>(field, chain, binding -> {
            binding.setRequired(required, errorMessage);
            doneHandler.accept(binding);
        });
    }
}
