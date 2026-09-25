package com.fortydash.domain.model;
import java.math.BigDecimal;

public class Expense {
    private String date;
    private String title;
    private BigDecimal amount;
    private String user;

    public Expense(String date, String title, BigDecimal amount, String user) {
        this.date = date;
        this.title = title;
        this.amount = amount;
        this.user = user;
    }
    public String getDate() { return date; }
    public String getTitle() { return title; }
    public BigDecimal getAmount() { return amount; }
    public String getUser() { return user; }
}