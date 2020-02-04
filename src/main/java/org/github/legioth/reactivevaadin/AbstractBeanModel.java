package org.github.legioth.reactivevaadin;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableSupplier;

/**
 * A collection of ModelProperty instances that can be populated from or
 * exported to a bean.
 * 
 * @param <T>
 *            the bean type
 */
public class AbstractBeanModel<T> {
    private final SerializableSupplier<T> constructor;
    private final T defaults;

    private final List<SerializableConsumer<T>> readers = new ArrayList<>();
    private final List<SerializableConsumer<T>> writers = new ArrayList<>();

    public AbstractBeanModel(SerializableSupplier<T> constructor) {
        this.constructor = constructor;
        defaults = constructor.get();
    }

    protected <V> Property<V> createProperty(SerializableFunction<T, V> getter) {
        ModelProperty<V> property = new ModelProperty<>();

        addProperty(bean -> {
            if (bean != null) {
                property.setValue(getter.apply(bean));
            } else {
                property.setValue(null);
            }
        }, null);

        return property;
    }

    protected <V> ModelProperty<V> createProperty(SerializableFunction<T, V> getter,
            SerializableBiConsumer<T, V> setter) {
        ModelProperty<V> property = new ModelProperty<>();

        addProperty(bean -> {
            if (bean != null) {
                property.setValue(getter.apply(bean));
            } else {
                property.setValue(null);
            }
        }, bean -> setter.accept(bean, property.subscribe().getValue()));

        return property;
    }

    protected <V, M extends AbstractBeanModel<V>> ModelList<M> createList(SerializableFunction<T, List<V>> getter,
            SerializableSupplier<M> modelFactory) {
        ModelList<M> reactiveList = new ModelList<>();

        addProperty(bean -> {
            reactiveList.clear();

            if (bean != null) {
                getter.apply(bean).stream().forEach(itemFromBean -> {
                    M model = modelFactory.get();
                    model.read(itemFromBean);
                    reactiveList.add(model);
                });
            }
        }, bean -> {
            List<V> list = getter.apply(bean);
            list.clear();

            reactiveList.stream().map(M::createAndWrite).forEach(list::add);
        });

        return reactiveList;
    }

    private void addProperty(SerializableConsumer<T> reader, SerializableConsumer<T> writer) {
        readers.add(reader);
        if (writer != null) {
            writers.add(writer);
        }
        reader.accept(defaults);
    }

    public void write(T bean) {
        writers.forEach(writer -> writer.accept(bean));
    }

    public void read(T bean) {
        readers.forEach(reader -> reader.accept(bean));
    }

    public T createAndWrite() {
        T instance = constructor.get();
        write(instance);
        return instance;
    }

    public void update(SerializableConsumer<T> updater) {
        T bean = createAndWrite();
        updater.accept(bean);
        read(bean);
    }

    public void reset() {
        read(defaults);
    }

}
