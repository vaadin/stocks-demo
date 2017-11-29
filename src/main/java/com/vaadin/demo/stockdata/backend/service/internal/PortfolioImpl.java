package com.vaadin.demo.stockdata.backend.service.internal;

import com.speedment.runtime.core.Speedment;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.investmentdata.InvestmentData;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.investmentdata.InvestmentDataImpl;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.investmentdata.InvestmentDataManager;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.SymbolManager;
import com.vaadin.demo.stockdata.backend.service.Investment;
import com.vaadin.demo.stockdata.backend.service.Portfolio;
import com.vaadin.demo.stockdata.backend.service.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class PortfolioImpl implements Portfolio {

    private final Service service;
    private final InvestmentDataManager investments;
    private final SymbolManager symbols;

    public PortfolioImpl(Service service, Speedment app) {
        this.service = service;
        investments = app.getOrThrow(InvestmentDataManager.class);
        symbols = app.getOrThrow(SymbolManager.class);
    }

    @Override
    public Stream<Investment> getInvestments() {
        return investments.stream()
            .map(investment ->
                new InvestmentImpl(service, investment.findSymbolId(symbols), investment.getQuantity()));
    }

    @Override
    public boolean removeInvestment(Symbol symbol) {
        List<InvestmentData> toRemove = investments.stream()
            .filter(InvestmentData.SYMBOL_ID.equal(symbol.getId()))
            .collect(Collectors.toList());

        toRemove.forEach(investments::remove);
        return !toRemove.isEmpty();
    }

    @Override
    public void addInvestment(Symbol symbol, long amount) {
        final InvestmentData investmentData = investments.stream()
            .filter(InvestmentData.SYMBOL_ID.equal(symbol.getId()))
            .findAny()
            .orElse(investments.persist(new InvestmentDataImpl()
                .setSymbolId(symbol.getId())));

        investmentData.setQuantity(Math.max(0, investmentData.getQuantity() + amount));

        investments.update(investmentData);
    }

    @Override
    public BigDecimal getCurrentValue(){
        return getInvestments()
            .map(Investment::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
