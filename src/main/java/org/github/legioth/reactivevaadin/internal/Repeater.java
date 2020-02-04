package org.github.legioth.reactivevaadin.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.github.legioth.reactivevaadin.Bindings;
import org.github.legioth.reactivevaadin.Property;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;

public class Repeater<T> {
    @SuppressWarnings("rawtypes")
    private static final SerializableBiConsumer NO_OP = (a, b) -> {
        // nop
    };

    private final SerializableFunction<T, ? extends Component> componentFactory;

    private final SerializableBiConsumer<Component, T> updater;
    private final SerializableBiConsumer<Component, T> remover;

    private final HashMap<T, Component> components = new HashMap<>();

    private final Component container;

    private Registration itemsRegistration;

    public Repeater(Component container, SerializableFunction<T, Component> componentFactory) {
        <Component> this(container, componentFactory, (SerializableBiConsumer<Component, T>) NO_OP);
    }

    public <C extends Component> Repeater(Component container, SerializableFunction<T, C> componentFactory,
            SerializableBiConsumer<C, T> updateHandler) {
        <C> this(container, componentFactory, updateHandler, (SerializableBiConsumer<C, T>) NO_OP);
    }

    @SuppressWarnings("unchecked")
    public <C extends Component> Repeater(Component container, SerializableFunction<T, C> componentFactory,
            SerializableBiConsumer<C, T> updateHandler, SerializableBiConsumer<C, T> removeHandler) {
        this.container = container;
        this.componentFactory = componentFactory;
        this.updater = (SerializableBiConsumer<Component, T>) updateHandler;
        this.remover = (SerializableBiConsumer<Component, T>) removeHandler;
    }

    private void refresh(List<T> items) {
        if (items.size() != new HashSet<>(items).size()) {
            throw new IllegalArgumentException("List contains non-distinct items");
        }
        Element containerElement = container.getElement();

        // Map old elements to their item
        HashMap<Element, T> elementToItem = new HashMap<>();
        components.forEach((item, component) -> elementToItem.put(component.getElement(), item));

        // Collect new index of all retained items and instantiate new ones
        HashMap<T, Integer> newIndices = new HashMap<>();
        for (T item : items) {
            if (components.containsKey(item)) {
                newIndices.put(item, Integer.valueOf(newIndices.size()));
                updater.accept(components.get(item), item);
            } else {
                components.put(item, componentFactory.apply(item));
            }
        }

        // Collect old index for all retained items and remove others
        HashMap<T, Integer> oldIndices = new HashMap<>();
        List<Element> toRemove = new ArrayList<>();
        containerElement.getChildren().forEach(child -> {
            T item = elementToItem.get(child);
            if (newIndices.containsKey(item)) {
                oldIndices.put(item, Integer.valueOf(oldIndices.size()));
            } else {
                remover.accept(components.remove(item), item);
                toRemove.add(child);
            }
        });

        // Remove from the end to play more nicely with the backing ArrayList
        for (int i = toRemove.size() - 1; i >= 0; i--) {
            containerElement.removeChild(toRemove.get(i));
        }

        // Put each element at its right location with minimal changes
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);

            Element child = components.get(item).getElement();
            Element currentContainerElement = i < containerElement.getChildCount() ? containerElement.getChild(i)
                    : null;

            if (!child.equals(currentContainerElement)) {
                /*
                 * We can either insert the child at current position or remove
                 * elements until child is in the right location. Those removed
                 * elements are added back at their right location when
                 * encountered in the items list.
                 * 
                 * We assume that only one small block of items needs to be
                 * moved. The block to move either starts with the current item
                 * or with the item at the current position in the container.
                 * 
                 * If we were to move the block starting at the current item,
                 * then the element at the current position in the container is
                 * offset from its desired location by the size of that block.
                 * If we were to move the block starting at the current position
                 * in the container, then the current item is offset by the size
                 * of that block. We choose to move the smaller block based on
                 * those offset sizes.
                 */
                boolean insert;
                if (currentContainerElement == null || !oldIndices.containsKey(item)) {
                    // No more elements in container or a new item
                    insert = true;
                } else {
                    T containerItem = elementToItem.get(currentContainerElement);

                    int containerOffset = getItemOffset(containerItem, newIndices, oldIndices);
                    int itemOffset = getItemOffset(item, newIndices, oldIndices);

                    insert = containerOffset <= itemOffset;
                }

                if (insert) {
                    containerElement.insertChild(i, child);
                } else {
                    while (!child.equals(containerElement.getChild(i))) {
                        containerElement.removeChild(i);
                    }
                }
            }
        }
    }

    private static <T> int getItemOffset(T item, HashMap<T, Integer> newIndices, HashMap<T, Integer> oldIndices) {
        return Math.abs(newIndices.get(item).intValue() - oldIndices.get(item).intValue());
    }

    public Registration setItems(Property<List<T>> items) {
        if (itemsRegistration != null) {
            itemsRegistration.remove();
        }

        // Property that is updated whenever item structure is changed or an
        // item is replaced

        Registration registration = Bindings.consume(items.beforeClientResponse(), container, this::refresh);

        itemsRegistration = registration;
        return registration;
    }
}
