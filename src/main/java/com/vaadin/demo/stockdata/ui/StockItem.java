package com.vaadin.demo.stockdata.ui;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.flow.component.charts.Chart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockItem {

  private String currentValue;
  private List<Double> history;
  private Symbol symbol;

  private static final Map<String, String> TRENDS;

  static {
    TRENDS = new HashMap<>();
    TRENDS.put("declining", "#ff0000cc");
    TRENDS.put("appreciating", "#00dd00cc");
  }

  public String getCurrentValue() {
    return currentValue;
  }

  public void setCurrentValue(String currentValue) {
    this.currentValue = currentValue;
  }

  public List<Double> getHistory() {
    return history;
  }

  public void setHistory(List<Double> history) {
    this.history = history;
  }

  public Symbol getSymbol() {
    return symbol;
  }

  public void setSymbol(Symbol symbol) {
    this.symbol = symbol;
  }

  public String getTrend() {
    int length = history.size();

    if(length >= 2){
      return history.get(length - 2) > history.get(length -1) ? "declining" : "appreciating";
    }
    return "appreciating";
  }

  public String getTrendColor() {
    return TRENDS.get(getTrend());
  }
}
