package org.github.legioth.reactivevaadin;

import java.math.BigDecimal;
import java.util.Random;

import org.github.legioth.reactivevaadin.Bindings;
import org.github.legioth.reactivevaadin.FormBindings;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.Command;

public class OrderForm extends VerticalLayout {
    private OrderModel model = new OrderModel();

    // @Autowire or @Inject in a real application
    private OrderService orderService = new OrderService();

    private Command submitCommand;
    private Button submitButton = new Button("", clickEvent -> submitCommand.execute());

    public OrderForm() {
        TextField customerName = new TextField("Customer name");
        BigDecimalField discount = new BigDecimalField("Discount");
        discount.setPrefixComponent(new Span("$"));
        VerticalLayout lines = new VerticalLayout();
        Span total = new Span();
        Span allInStock = new Span("All products are in stock");

        FormBindings form = new FormBindings();
        form.bind(customerName, model.customerName).setRequired("Must enter customer name");
        form.forField(discount).withConverter(OrderForm::adjustScale, modelValue -> modelValue)
                .withValidator(value -> value.equals(value.abs()), "Discount cannot be negative").bind(model.discount);

        Bindings.repeat(lines, model.lines, line -> new OrderLineForm(line, form, () -> model.lines.remove(line)));

        Button addOrderLine = new Button("+", clickEvent -> {
            OrderLineModel newLine = new OrderLineModel();
            newLine.product.setValue(orderService.getDefaultProduct());
            newLine.amount.setValue(1);
            model.lines.add(newLine);
        });

        Bindings.bindEnabled(submitButton, form.hasChanges.and(form.isValid));

        Bindings.bindText(total, "Total $%s - $%s = $%s", model.linePriceSum, model.discount, model.totalPrice);
        Bindings.bindVisible(allInStock, model.allInStock);

        add(customerName, discount, lines, addOrderLine, allInStock, total, submitButton);

        // Initialize submit button text and logic
        // setOrder(null);

        setOrder(OrderService.createOrder(new Random(42)));
    }

    private static BigDecimal adjustScale(BigDecimal fieldValue) {
        if (fieldValue == null) {
            fieldValue = BigDecimal.ZERO;
        }
        return fieldValue.setScale(2);
    }

    public void setOrder(Order order) {
        if (order == null) {
            model.reset();
            submitButton.setText("Create order");
            submitCommand = () -> {
                Order newOrder = model.createAndWrite();
                orderService.saveNewOrder(newOrder);
            };
        } else {
            model.read(order);
            submitButton.setText("Update order");
            submitCommand = () -> {
                model.write(order);
                orderService.updateExistingOrder(order);
            };
        }
    }
}
