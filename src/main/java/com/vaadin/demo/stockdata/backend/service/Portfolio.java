package com.vaadin.demo.stockdata.backend.service;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

public interface Portfolio {
    /**
     * @return a stream of the investments of the portfolio
     */
    Stream<Investment> getInvestments();

    /**
     * Remove all investments for a given symbol
     *
     * @param symbol the symbol for which the investments shall be removed
     * @return true iff any investments were removed
     */
    boolean removeInvestment(Symbol symbol);

    /**
     * Add an investment to the portfolio. If there are investments for the
     * given symbol, the amount is added to the existing number of shares.
     *
     * @param symbol the symbol of the investment to add
     * @param amount the number of shares to add (negative values allowed)
     */
    void addInvestment(Symbol symbol, long amount);

    /**
     * @return the current total market value of the portfolio
     */
    BigDecimal getCurrentValue();
}
