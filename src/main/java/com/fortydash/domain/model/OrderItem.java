package com.fortydash.domain.model;
import java.math.BigDecimal;
import javafx.beans.property.*;

public class OrderItem {
    private String barcode;
    private String name;
    private IntegerProperty qty;
    private BigDecimal price;
    private BigDecimal cost;
    private ObjectProperty<BigDecimal> total;

    public OrderItem(String barcode, String name, BigDecimal price, BigDecimal cost, int q) {
        this.barcode = barcode;
        this.name = name;
        this.price = price;
        this.cost = cost;
        this.qty = new SimpleIntegerProperty(q);
        this.total = new SimpleObjectProperty<>(price.multiply(BigDecimal.valueOf(q)));
    }
    public String getBarcode() { return barcode; }
    public String getName() { return name; }
    public int getQty() { return qty.get(); }
    public void setQty(int q) {
        this.qty.set(q);
        this.total.set(price.multiply(BigDecimal.valueOf(q)));
    }
    public IntegerProperty qtyProperty() { return qty; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getCost() { return cost; }
    public BigDecimal getTotal() { return total.get(); }
    public ObjectProperty<BigDecimal> totalProperty() { return total; }
}