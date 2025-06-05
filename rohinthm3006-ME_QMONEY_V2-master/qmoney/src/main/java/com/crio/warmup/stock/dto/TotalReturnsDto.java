package com.crio.warmup.stock.dto;

import java.util.Comparator;

public class TotalReturnsDto {

    private String symbol;
    private Double closingPrice;

    // Constructor
    public TotalReturnsDto(String symbol, Double closingPrice) {
        this.symbol = symbol;
        this.closingPrice = closingPrice;
    }

    // Overloaded Constructor
    public TotalReturnsDto(String symbol) {
        this.symbol = symbol;
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Double getClosingPrice() {
        return closingPrice;
    }

    public void setClosingPrice(Double closingPrice) {
        this.closingPrice = closingPrice;
    }

    // Comparator for Sorting
    public static final Comparator<TotalReturnsDto> closingComparator = Comparator.comparing(TotalReturnsDto::getClosingPrice);

    // toString Method
    @Override
    public String toString() {
        return "TotalReturnsDto{" +
                "symbol='" + symbol + '\'' +
                ", closingPrice=" + closingPrice +
                '}';
    }



}
