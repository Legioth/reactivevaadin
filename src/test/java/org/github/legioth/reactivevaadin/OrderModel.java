package org.github.legioth.reactivevaadin;

import java.math.BigDecimal;

import org.github.legioth.reactivevaadin.Order.Product;

public class OrderModel extends AbstractBeanModel<Order> {
    /* Actual properties */
    public final ModelProperty<String> customerName = createProperty(Order::getCustomerName, Order::setCustomerName);
    public final ModelProperty<BigDecimal> discount = createProperty(Order::getDiscount, Order::setDiscount);
    public final ModelList<OrderLineModel> lines = createList(Order::getOrderLines, OrderLineModel::new);

    /* Derived properties */
    public final Flag hasDiscount = discount.test(new BigDecimal("0.00")::equals).not();
    public final Property<BigDecimal> linePriceSum = lines.mapProperties(line -> line.price).reduce(BigDecimal.ZERO,
            BigDecimal::add);
    public final Property<BigDecimal> totalPrice = linePriceSum.combine(discount, BigDecimal::subtract);
    public final Flag allInStock = lines.mapProperties(line -> line.product).allMatch(Product::isInStock);

    public OrderModel() {
        super(Order::new);
    }
}
