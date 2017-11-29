package com.vaadin.demo.stockdata.backend.service;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

public interface Investment {
    /**
     * @return the symbol of the investment
     */
    Symbol getSymbol();

    /**
     * @return the amount of the investment
     */
    long getAmount();

    /**
     * @return a stream of the data points of the symbol for the last week
     */
    Stream<DataPoint> getLastWeekOverview();

    /**
     * @return the current value of the data point
     */
    BigDecimal getCurrentValue();
}