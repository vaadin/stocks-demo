package com.vaadin.demo.stockdata.backend.entity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class Investment {
    private Symbol symbol;
    private long shares;
    /** Data for sparkline */
    private transient List<DataPoint> lastWeekOverview = Collections.emptyList();
    private BigDecimal currentValue;

    public Symbol getSymbol() {
        return symbol;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    public long getShares() {
        return shares;
    }

    public void setShares(long shares) {
        this.shares = shares;
    }

    public List<DataPoint> getLastWeekOverview() {
        return lastWeekOverview;
    }

    public void setLastWeekOverview(List<DataPoint> lastWeekOverview) {
        this.lastWeekOverview = lastWeekOverview;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }
}
