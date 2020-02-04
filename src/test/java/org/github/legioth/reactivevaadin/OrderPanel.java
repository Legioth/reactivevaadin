package org.github.legioth.reactivevaadin;

import org.github.legioth.reactivevaadin.Bindings;
import org.github.legioth.reactivevaadin.ModelProperty;
import org.github.legioth.reactivevaadin.Order.Product;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class OrderPanel extends VerticalLayout {

    private OrderModel model = new OrderModel();

    public OrderPanel() {
        Span discount = new Span();
        Span customerName = new Span();
        Span total = new Span();
        Span allInStock = new Span("All products are in stock");

        Bindings.bindText(customerName, "Customer: %s", model.customerName);
        Bindings.bindText(discount, "Discount: $%s", model.discount);

        Bindings.bindClassName(discount, "discount", model.hasDiscount.and(highlightDiscount));

        Div orderLines = new Div();
        Bindings.repeat(orderLines, model.lines, model -> {
            Paragraph lineComponent = new Paragraph();
            Bindings.bindText(lineComponent, "%s %s x $%s = $%s", model.product.map(Product::getName), model.amount,
                    model.product.map(Product::getPrice), model.price);
            return lineComponent;
        });

        Bindings.bindVisible(allInStock, model.allInStock);
        Bindings.bindText(total, "Total $%s - $%s = $%s", model.linePriceSum, model.discount, model.totalPrice);

        add(customerName, discount, orderLines, allInStock, total);
    }

    public void setOrder(Order order) {
        model.read(order);
    }

    private ModelProperty<Boolean> highlightDiscount = new ModelProperty<>(true);

    public void setHighlightDiscount(boolean highlightDiscount) {
        this.highlightDiscount.setValue(highlightDiscount);
    }

    // Just for the sake of the demo
    public OrderModel getModel() {
        return model;
    }
}
