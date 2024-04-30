package com.alphatica.alis.trading.account;

public class Position {
    private double size;
    private double lastPrice;

    public Position(double size) {
        this.size = size;
    }

    public double getSize() {
        return size;
    }

    public double updateQuantity(double quantityChange) {
        size += quantityChange;
        return size;
    }

    public void updateLastPrice(double price) {
        lastPrice = price;
    }

    public double getLastPrice() {
        return lastPrice;
    }
}
