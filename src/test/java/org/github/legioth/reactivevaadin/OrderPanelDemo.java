package org.github.legioth.reactivevaadin;

import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.github.legioth.reactivevaadin.Bindings;
import org.github.legioth.reactivevaadin.ModelList;
import org.github.legioth.reactivevaadin.ModelProperty;
import org.github.legioth.reactivevaadin.Property;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;

@Route("panel")
public class OrderPanelDemo extends Div {
    private final OrderPanel panel = new OrderPanel();

    public OrderPanelDemo() {
        Order order = OrderService.createOrder(new Random(0));
        panel.setOrder(order);

        Div demoControls = new Div(new Button("setOrder", event -> {
            panel.setOrder(OrderService.createOrder(ThreadLocalRandom.current()));
        }), new Button("Update customer", event -> {
            ModelProperty<String> customerName = (ModelProperty<String>) panel.getModel().customerName;
            customerName.setValue("Custom customer");
        }), new Checkbox("Highlight discount", event -> {
            panel.setHighlightDiscount(event.getValue());
        }) {
            {
                setValue(true);
            }
        }, requireMinLineCount(1, new Button("Remove last order line", event -> {
            ModelList<OrderLineModel> lines = panel.getModel().lines;
            lines.remove(lines.size() - 1);
        })), new Button("Add order line", event -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();

            OrderLineModel model = new OrderLineModel();
            model.read(OrderService.createOrderLine(random));

            ModelList<OrderLineModel> lines = panel.getModel().lines;
            lines.add(random.nextInt(lines.size() + 1), model);
        }), requireMinLineCount(2, new Button("Swap order lines", event -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            ModelList<OrderLineModel> lines = panel.getModel().lines;
            int from = random.nextInt(lines.size());
            int to = random.nextInt(lines.size() - 1);
            if (to >= from) {
                to++;
            }
            Collections.swap(lines, from, to);
        })), requireMinLineCount(1, new Button("Increment order amount", event -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            ModelList<OrderLineModel> lines = panel.getModel().lines;

            ModelProperty<Integer> amount = (ModelProperty<Integer>) lines.get(random.nextInt(lines.size())).amount;
            amount.update(value -> value + 1);
        })));

        add(demoControls, panel);

        // When you're too lazy to create a separate .css file. Wrapping div
        // needed because of https://github.com/vaadin/flow/pull/7466
        add(new Html("<div><style>.discount {font-weight: bold}"));
    }

    private Component requireMinLineCount(int minLineCount, Component compnent) {
        return bindEnabled(compnent, panel.getModel().lines.count().test(count -> count >= minLineCount));
    }

    private Component bindEnabled(Component component, Property<Boolean> flag) {
        Bindings.bindEnabled(component, flag);
        return component;
    }
}
