package com.vaadin.demo.stockdata.backend.service.internal;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.demo.stockdata.backend.service.Investment;
import com.vaadin.demo.stockdata.backend.service.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

public final class InvestmentImpl implements Investment {

    private final Service service;
    private final Symbol symbol;
    private final long amount;

    InvestmentImpl(Service service, Symbol symbol, long amount) {
        this.service = service;
        this.symbol = symbol;
        this.amount = amount;
    }

    @Override
    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public Stream<DataPoint> getLastWeekOverview() {
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusWeeks(1);
        return service.getHistoryData(symbol, start, now);
    }

    @Override
    public BigDecimal getCurrentValue() {
        final Optional<DataPoint> pointOptional = service.getMostRecentDataPoint(symbol);
        if (pointOptional.isPresent()) {
            return new BigDecimal(amount)
                .multiply(BigDecimal.valueOf(pointOptional.get().getClose()));
        } else {
            return BigDecimal.ZERO;
        }
    }
}