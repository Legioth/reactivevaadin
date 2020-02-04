package org.github.legioth.reactivevaadin;

import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializablePredicate;

public interface FieldBindingBuilder<P, F> {

    <C> FieldBindingBuilder<C, F> withConverter(SerializableFunction<P, C> toModel, SerializableFunction<C, P> toField,
            String errorMessage);

    default <C> FieldBindingBuilder<C, F> withConverter(SerializableFunction<P, C> toModel,
            SerializableFunction<C, P> toField) {
        return withConverter(toModel, toField, null);
    }

    FieldBindingBuilder<P, F> withValidator(SerializablePredicate<P> test, String errorMessage);

    FieldBinding<P, F> bind(ModelProperty<P> property);

    FieldBindingBuilder<P, F> asRequired(String errorMessage);

    FieldBindingBuilder<P, F> asRequired(Property<Boolean> required, String errorMessage);
}
