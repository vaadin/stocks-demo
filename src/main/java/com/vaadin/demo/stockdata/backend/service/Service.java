package com.vaadin.demo.stockdata.backend.service;

import com.vaadin.demo.stockdata.backend.entity.DataPoint;
import com.vaadin.demo.stockdata.backend.entity.Portfolio;
import com.vaadin.demo.stockdata.backend.entity.Symbol;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Service for managing a user's portfolio and getting historical data.
 * This demo app has no concept of users, so the entire database is a
 * single user.
 */
public class Service {

    /**
     * @return the demo user's portfolio
     */
    public Portfolio getPortfolio() {
        return new Portfolio();
    }

    /**
     * @param symbol Symbol we want history data for.
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of at most 500 {@link DataPoint}s for the given interval.
     */
    public List<DataPoint> getHistoryData(Symbol symbol, LocalDate startDate, LocalDate endDate) {
        return Collections.emptyList();
    }
}
