package org.github.legioth.reactivevaadin;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.github.legioth.reactivevaadin.Order.OrderLine;
import org.github.legioth.reactivevaadin.Order.Product;

public class OrderService {
    private static Product vanillaCracker = new Product("Vanilla cracker", BigDecimal.valueOf(1234, 2), true);
    private static Product strawberryBun = new Product("Stawberry bun", new BigDecimal("8.39"), false);

    public static Order createOrder(Random random) {
        Order order = new Order();

        order.setCustomerName("Customer " + random.nextInt(20));

        order.setDiscount(new BigDecimal(random.nextDouble() < 0.75 ? "5.00" : "0"));

        if (random.nextBoolean()) {
            order.getOrderLines().add(createOrderLine(random));
        } else {
            order.getOrderLines().add(new OrderLine(vanillaCracker, 1 + random.nextInt(4)));
            order.getOrderLines().add(new OrderLine(strawberryBun, 1 + random.nextInt(8)));
        }

        return order;
    }

    public static OrderLine createOrderLine(Random random) {
        return new OrderLine(getRandomProduct(random), 1 + random.nextInt(6));
    }

    private static Product getRandomProduct(Random random) {
        return random.nextBoolean() ? vanillaCracker : strawberryBun;
    }

    public static List<Product> getProducts() {
        return Arrays.asList(vanillaCracker, strawberryBun);
    }

    public void saveNewOrder(Order newOrder) {
        // TODO Auto-generated method stub

    }

    public void updateExistingOrder(Order order) {
        // TODO Auto-generated method stub

    }

    public Product getDefaultProduct() {
        return vanillaCracker;
    }
}
