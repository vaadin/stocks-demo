package com.vaadin.demo.stockdata.ui;

import com.vaadin.flow.component.charts.Chart;

import java.util.HashMap;
import java.util.Map;

public class StockItem extends Chart {

    private static final Map<String, String> TRENDS;

    static {
        TRENDS = new HashMap<>();
        TRENDS.put("declining", "#ff0000cc");
        TRENDS.put("appreciating", "#00dd00cc");
    }

    private final String nasdaqCode;
    private final int numOfShares;
    private final double price;
    private final double[] historicalData;

    public StockItem(String nasdaqCode, int numOfShares, double price, double[] historicalData) {
        super();
        this.nasdaqCode = nasdaqCode;
        this.numOfShares = numOfShares;
        this.price = price;
        this.historicalData = historicalData;
    }

    public String getNasdaqCode() {
        return nasdaqCode;
    }

    public int getNumOfShares() {
        return numOfShares;
    }

    public String getPrice() {
        return String.format("%.2f", price);
    }

    public double[] getHistoricalData() {
        return historicalData;
    }

    public String getTrend() {
        int length = historicalData.length;
        return historicalData[length - 2] > historicalData[length - 1] ? "declining" : "appreciating";
    }

    public String getTrendColor() {
        return TRENDS.get(getTrend());
    }
}
