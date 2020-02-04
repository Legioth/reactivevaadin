package org.github.legioth.reactivevaadin;

import java.util.concurrent.ThreadLocalRandom;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("form")
public class OrderFormDemo extends VerticalLayout {
    public OrderFormDemo() {
        OrderForm form = new OrderForm();

        Div demoControls = new Div(new Button("Create new", event -> {
            form.setOrder(null);
        }), new Button("Edit existing", event -> {
            form.setOrder(OrderService.createOrder(ThreadLocalRandom.current()));
        }));
        add(demoControls, form);
    }
}
