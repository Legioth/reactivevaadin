package org.github.legioth.reactivevaadin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.github.legioth.reactivevaadin.internal.ChainResult;
import org.github.legioth.reactivevaadin.internal.FieldBindingBuilderImpl;
import org.github.legioth.reactivevaadin.internal.FieldBindingChain;
import org.github.legioth.reactivevaadin.internal.SubscriptionContext;

import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.shared.Registration;

public class FieldBinding<P, F> implements Registration {

    private final ModelProperty<P> property;
    private final HasValueAndElement<?, F> field;
    private final FieldBindingChain<P, F> chain;

    private final List<Registration> unregisterListeners = new ArrayList<>();

    private final ModelProperty<P> lastValueFromProperty;

    public final Flag hasChanges;

    public final ModelProperty<Boolean> isValid = new ModelProperty<>(Boolean.TRUE);

    private String requiredMessage;

    private boolean ignorePropertyChange = false;

    public FieldBinding(ModelProperty<P> property, FieldBindingChain<P, F> chain, HasValueAndElement<?, F> field) {
        this.property = property;
        this.chain = chain;
        this.field = field;

        lastValueFromProperty = new ModelProperty<>();

        P initialPropertyValue = property.subscribe().getValue();
        updateValueFromProperty(initialPropertyValue);

        hasChanges = property.isEqual(lastValueFromProperty).not();

        addUnregisterListener(SubscriptionContext.forComponent(field).addAttachListener(ui -> {
            Registration fieldRegistration = field
                    .addValueChangeListener(event -> updateValueFromField(event.getValue()));

            Registration propertyRegistration = Bindings.consume(property, field,
                    value -> updateValueFromProperty(value));

            return () -> {
                fieldRegistration.remove();
                propertyRegistration.remove();
            };
        }));
    }

    private void updateValueFromField(F newValue) {
        ChainResult<P> result;
        if (requiredMessage != null && field.isEmpty()) {
            result = ChainResult.error(requiredMessage);
        } else {
            // TODO check field's own status
            result = chain.toProperty(ChainResult.value(newValue));
        }

        // TODO check property validator (should this maybe be built in to the
        // chain?)

        if (result.getError() == null) {
            ChainResult<F> fieldResult = chain.toField(result);
            if (fieldResult.getError() == null && !Objects.equals(newValue, fieldResult.getValue())) {
                field.setValue(fieldResult.getValue());
                // setValue ought to call updateValueFromField again
                return;
            }

            isValid.setValue(Boolean.TRUE);
            if (field instanceof HasValidation) {
                ((HasValidation) field).setErrorMessage(null);
                ((HasValidation) field).setInvalid(false);
            }

            try {
                ignorePropertyChange = true;
                property.setValue(result.getValue());
            } finally {
                ignorePropertyChange = false;
            }
        } else {
            isValid.setValue(Boolean.FALSE);
            if (field instanceof HasValidation) {
                // TODO Define as reactive value for more flexibility
                ((HasValidation) field).setErrorMessage(result.getError());
                ((HasValidation) field).setInvalid(true);
            }
        }
    }

    private void updateValueFromProperty(P newValue) {
        if (ignorePropertyChange) {
            return;
        }

        lastValueFromProperty.setValue(newValue);

        // TODO check property validator
        ChainResult<F> result = chain.toField(ChainResult.value(newValue));
        if (result.getError() == null) {
            // XXX Lots of code duplicated compared from the opposite case
            isValid.setValue(Boolean.TRUE);
            if (field instanceof HasValidation) {
                ((HasValidation) field).setErrorMessage(null);
                ((HasValidation) field).setInvalid(false);
            }

            // TODO update status flags
            field.setValue(result.getValue());
            // TODO check field validator
        } else {
            isValid.setValue(Boolean.FALSE);
            if (field instanceof HasValidation) {
                // TODO Define as reactive value for more flexibility
                ((HasValidation) field).setErrorMessage(result.getError());
                ((HasValidation) field).setInvalid(true);
            }
        }
    }

    public void setRequired(String requiredMessage) {
        if (!Objects.equals(this.requiredMessage, requiredMessage)) {
            this.requiredMessage = requiredMessage;

            field.setRequiredIndicatorVisible(requiredMessage != null);
            // Just trigger running the validation logic, this could be more
            // selective
            updateValueFromField(field.getValue());
        }
    }

    public void setRequired(Property<Boolean> requiredProperty, String errorMessage) {
        /*
         * TODO make regular setRequired unregister this binding. This also
         * implies that this binding should delegate to a separate method
         */
        addUnregisterListener(Bindings.consume(requiredProperty, field,
                required -> setRequired(Boolean.TRUE.equals(required) ? errorMessage : null)));
    }

    public Registration addUnregisterListener(Registration unregisterListener) {
        unregisterListeners.add(unregisterListener);
        return () -> unregisterListeners.remove(unregisterListener);
    }

    @Override
    public void remove() {
        new ArrayList<>(unregisterListeners).forEach(listener -> listener.remove());
    }

    public HasValueAndElement<?, F> getField() {
        return field;
    }

    public static <T> FieldBindingBuilder<T, T> build(HasValueAndElement<?, T> field) {
        return new FieldBindingBuilderImpl<>(field, FieldBindingChain.empty(), ignore -> {
            // nop
        });
    }

}
