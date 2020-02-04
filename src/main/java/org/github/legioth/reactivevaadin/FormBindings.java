package org.github.legioth.reactivevaadin;

import org.github.legioth.reactivevaadin.internal.FieldBindingBuilderImpl;
import org.github.legioth.reactivevaadin.internal.FieldBindingChain;
import org.github.legioth.reactivevaadin.internal.SubscriptionContext;

import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.shared.Registration;

public class FormBindings {
    private final ModelList<FieldBinding<?, ?>> activeBindings = new ModelList<>();

    public Flag isValid = activeBindings.mapProperties(binding -> binding.isValid).allMatch(Boolean.TRUE::equals);
    public Flag hasChanges = activeBindings.mapProperties(binding -> binding.hasChanges).anyMatch(Boolean.TRUE::equals);

    public FormBindings() {

    }

    public <T> FieldBinding<T, T> bind(HasValueAndElement<?, T> field, ModelProperty<T> property) {
        return forField(field).bind(property);
    }

    public <T> FieldBindingBuilder<T, T> forField(HasValueAndElement<?, T> field) {
        return new FieldBindingBuilderImpl<>(field, FieldBindingChain.empty(), this::addBinding);
    }

    public Registration addBinding(FieldBinding<?, ?> binding) {
        Registration attachRegistration = SubscriptionContext.forComponent(binding.getField()).addAttachListener(ui -> {
            activeBindings.add(binding);
            return () -> activeBindings.remove(binding);
        });

        Registration unregisterListener = binding.addUnregisterListener(attachRegistration);

        return () -> {
            attachRegistration.remove();
            unregisterListener.remove();
        };
    }

    public boolean isValid() {
        return Boolean.TRUE.equals(isValid.subscribe().getValue());
    }
}
