package org.github.legioth.reactivevaadin;

import org.github.legioth.reactivevaadin.internal.CallbackSubscription;
import org.github.legioth.reactivevaadin.internal.Subscription;

import com.vaadin.flow.component.HasValue;

public class FieldProperty<T> implements Property<T> {

    private final HasValue<?, T> field;

    public FieldProperty(HasValue<?, T> field) {
        this.field = field;
    }

    @Override
    public Subscription<T> subscribe() {
        return new CallbackSubscription<>(field::getValue,
                command -> field.addValueChangeListener(event -> command.execute()));
    }
}
