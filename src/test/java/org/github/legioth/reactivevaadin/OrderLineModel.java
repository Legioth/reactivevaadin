package org.github.legioth.reactivevaadin;

import java.math.BigDecimal;

import org.github.legioth.reactivevaadin.Order.OrderLine;
import org.github.legioth.reactivevaadin.Order.Product;

public class OrderLineModel extends AbstractBeanModel<OrderLine> {
    public final ModelProperty<Product> product = createProperty(OrderLine::getProduct, OrderLine::setProduct);
    public final ModelProperty<Integer> amount = createProperty(OrderLine::getAmount, OrderLine::setAmount);

    public final Property<BigDecimal> price = product.map(Product::getPrice).combine(amount.map(BigDecimal::valueOf),
            BigDecimal::multiply);

    public OrderLineModel() {
        super(OrderLine::new);
    }
}