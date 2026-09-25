package com.fortydash.domain.model;
import java.math.BigDecimal;

public class Product {
    private String barcode;
    private String name;
    private BigDecimal price;
    private BigDecimal cost;
    private int quantity;

    public Product(String barcode, String name, BigDecimal price, BigDecimal cost, int quantity) {
        this.barcode = barcode;
        this.name = name;
        this.price = price;
        this.cost = cost;
        this.quantity = quantity;
    }
    public String getBarcode() { return barcode; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getCost() { return cost; }
    public int getQuantity() { return quantity; }
}