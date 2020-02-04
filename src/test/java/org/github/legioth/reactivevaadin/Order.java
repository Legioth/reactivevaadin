package org.github.legioth.reactivevaadin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Order {
    public static class Product {
        private final String name;
        private final BigDecimal price;

        private final boolean inStock;

        public Product(String name, BigDecimal price, boolean inStock) {
            this.name = name;
            this.price = price;
            this.inStock = inStock;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public String getName() {
            return name;
        }

        public boolean isInStock() {
            return inStock;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    public static class OrderLine {
        private int id;
        private Product product;
        private int amount;

        public OrderLine() {
        }

        public OrderLine(Product product, int amount) {
            this.product = product;
            this.amount = amount;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Product getProduct() {
            return product;
        }

        public void setProduct(Product product) {
            this.product = product;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }
    }

    private String customerName = "";
    BigDecimal discount = new BigDecimal("0.00");
    private List<OrderLine> orderLines = new ArrayList<>();

    public String getCustomerName() {
        return customerName;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public List<OrderLine> getOrderLines() {
        return orderLines;
    }
}