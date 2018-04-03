package com.vaadin.demo.stockdata.backend.service;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.demo.stockdata.backend.service.internal.ServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Service for  getting historical data.
 * This demo app has no concept of users, so the entire database is a
 * single user.
 */
public interface Service {

    /**
     * Create a new Service
     *
     * @param hostIp database IP address
     * @param user database user name
     * @param password database password
     * @return a Service using the given credentials
     */
    static Service create(String hostIp, String user, String password) {
        return new ServiceImpl(hostIp, user, password);
    }


    /**
     * Retrieve historical data points for a given symbol.
     *
     * @param symbol Symbol we want history data for.
     * @param startTime Start time (inclusive)
     * @param endTime End time (inclusive)
     * @return The stream of all data points of the given symbol and time interval
     */
    Stream<DataPoint> getHistoryData(Symbol symbol, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * @param symbol the symbol for which to get data
     * @return the most recent data point for the given symbol
     */
    Optional<DataPoint> getMostRecentDataPoint(Symbol symbol);

    /**
     * @return the stream of all symbols of the database
     */
    Stream<Symbol> getSymbols();

    /**
     * Select whether to use Speedment in memory acceleration when looking up data
     *
     * @param accelerate true for using Speedment in memory acceleration, false for direct SQL
     */
    Service withAcceleration(boolean accelerate);
}
