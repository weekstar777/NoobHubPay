package com.noobhub.pay.models;

import java.util.List;

public class Product {
    private String name;
    private double price;
    private List<String> commands;
    private String description;

    public Product(String name, double price, List<String> commands, String description) {
        this.name = name;
        this.price = price;
        this.commands = commands;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public List<String> getCommands() {
        return commands;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
