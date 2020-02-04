package org.github.legioth.reactivevaadin;

import org.github.legioth.reactivevaadin.Bindings;
import org.github.legioth.reactivevaadin.FormBindings;
import org.github.legioth.reactivevaadin.Order.Product;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.server.Command;

public class OrderLineForm extends HorizontalLayout {

    public OrderLineForm(OrderLineModel model, FormBindings form, Command removeCommand) {
        setAlignItems(Alignment.BASELINE);
        ComboBox<Product> product = new ComboBox<>("Product", OrderService.getProducts());
        IntegerField amount = new IntegerField("Amount");
        amount.setHasControls(true);
        amount.setMin(1);
        Span totalPrice = new Span();
        Button remove = new Button("X", event -> removeCommand.execute());

        form.bind(product, model.product);
        form.bind(amount, model.amount);

        Bindings.bindText(totalPrice, " x $%s = $%s", model.product.map(Product::getPrice), model.price);

        add(product, amount, totalPrice, remove);
    }
}
