package org.github.legioth.reactivevaadin;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.github.legioth.reactivevaadin.internal.CombineSubscription;
import org.github.legioth.reactivevaadin.internal.Repeater;
import org.github.legioth.reactivevaadin.internal.SubscriptionContext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

/**
 * Static helpers for creating various bindings from a property.
 */
public class Bindings {

    public static <T, C extends HasElement> Registration consume(Property<T> property, C context,
            SerializableBiConsumer<C, T> consumer) {
        return property.subscribe().enable(subscription -> consumer.accept(context, subscription.getValue()),
                SubscriptionContext.forComponent(context));
    }

    public static <T> Registration consume(Property<T> property, HasElement context, SerializableConsumer<T> consumer) {
        return consume(property, context, (ignore, value) -> consumer.accept(value));
    }

    public static Registration bindText(HasText component, Property<?> property) {
        return consume(property.map(String::valueOf), component, HasText::setText);
    }

    public static Registration bindText(HasText component, String formatString, Property<?> property) {
        return bindText(component, property.format(formatString));
    }

    public static Registration bindText(HasText component, String formatString, Property<?>... properties) {
        return bindText(component, CombineSubscription
                .combine(usage -> String.format(formatString, Stream.of(properties).map(usage::valueOf).toArray())));
    }

    public static Registration bindEnabled(Component component, Property<Boolean> property) {
        return consume(property, component, component.getElement()::setEnabled);
    }

    public static Registration bindVisible(Component component, Property<Boolean> property) {
        return consume(property, component, Component::setVisible);
    }

    public static Registration bindClassName(HasElement component, String className, Property<Boolean> property) {
        return consume(property, component,
                (target, enabled) -> target.getElement().getClassList().set(className, Boolean.TRUE.equals(enabled)));
    }

    public static Registration ifTrue(HasElement context, Property<Boolean> flag, Command command) {
        return Bindings.consume(flag, context, value -> {
            if (Boolean.TRUE.equals(value)) {
                command.execute();
            }
        });
    }

    public static Registration elseDo(HasElement context, Property<Boolean> flag, Command command) {
        return ifTrue(context, Flag.fromProperty(flag).not(), command);
    }

    public static <T> Registration repeat(Component target, StreamProperty<T> properties,
            SerializableFunction<T, Component> componentFactory) {
        return repeat(target, properties.collect(Collectors.toList()), componentFactory);
    }

    public static <T> Registration repeat(Component target, Property<List<T>> properties,
            SerializableFunction<T, Component> componentFactory) {
        return new Repeater<>(target, componentFactory).setItems(properties);
    }
}
