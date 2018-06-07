package com.vaadin.demo.stockdata.backend.service.internal;

import com.speedment.enterprise.datastore.runtime.DataStoreBundle;
import com.speedment.enterprise.datastore.runtime.DataStoreComponent;
import com.speedment.enterprise.datastore.runtime.collector.SieveCollector;
import com.speedment.enterprise.virtualcolumn.runtime.VirtualColumnBundle;
import com.speedment.runtime.core.ApplicationBuilder;
import com.speedment.runtime.core.Speedment;
import com.speedment.runtime.field.predicate.Inclusion;
import com.vaadin.demo.stockdata.backend.db.StockdataApplication;
import com.vaadin.demo.stockdata.backend.db.StockdataApplicationBuilder;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPointManager;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.SymbolManager;
import com.vaadin.demo.stockdata.backend.service.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ServiceImpl implements Service {

    private static final String LICENSE_KEY = "HhD32Q3KophEmEYFYXBpLW1hbmFnZXIsZGF0YXN0b3JlLGRiMixtc3NxbCxvcmFjbGUscmVhY3Rvcix2aXJ0dWFsLWNvbHVtbnM7PG7WjvzDDv08zjh1gSR7EzdSBlFloOYuvMGIz1/+gsGClX0w/u9o+5hl6I6lVgz20/HP8K81NGHUUiU+lW/Hf3wM4RJybQ6be9OjgKa86aDwjXGGi8k8J/CzAx/vk7YZMUSXp1pRTxrRoRT7FpkHg0sKH2qM2kTR6tyfph8mC5I=";

    /**
     * The maximum granularity step of the batch. If the user requests a large range and actual data is confined in a small
     * sub sequence of the requested range, the granularity given by the large range will sieve out too much data yielding
     * a very small batch. Setting this value to at least one data point per day avoids this problem.
     *
     * Since the end points of available data will always be included no matter the granularity, we could also
     * to resend the request with the end points defining the range if the returned batch size
     * is inadequate, so this is just a way to avoid the double round trip to the source of the data.
     */
    private static final int MAXIMUM_GRANULARITY_STEP = (int) TimeUnit.SECONDS.convert(1, TimeUnit.DAYS);

    private final Speedment sqlApp;
    private final Speedment acceleratedApp;
    private DataPointManager dataPoints;
    private final String user;
    private final String password;
    private final String hostIp;

    public ServiceImpl(String hostIp, String user, String password) {
        this.hostIp = hostIp;
        this.user = user;
        this.password = password;
        sqlApp = createApp(false);
        acceleratedApp = createApp(true);
        dataPoints = getDataPointManager(true);
    }

    private Speedment createApp(boolean withAccelleration) {
        StockdataApplicationBuilder builder = new StockdataApplicationBuilder()
            .withUsername(user)
            .withPassword(password)
            .withIpAddress(hostIp)
            .withLogging(ApplicationBuilder.LogType.STREAM);

        if (withAccelleration) {
            builder = builder
                .withParam("licenseKey", LICENSE_KEY)
                .withBundle(VirtualColumnBundle.class)
                .withBundle(DataStoreBundle.class);
        }

        final StockdataApplication application = builder.build();

        if (withAccelleration) {
            DataStoreComponent dataStoreComponent = application.getOrThrow(DataStoreComponent.class);
            Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(dataStoreComponent::load,2, 2, TimeUnit.MINUTES);
            dataStoreComponent.load();  // make first load in current Thread to ensure we have loaded when returning
        }

        return application;
    }

    @Override
    public Service withAcceleration(boolean accelerate) {
        dataPoints = getDataPointManager(accelerate);
        return this;
    }

    private DataPointManager getDataPointManager(boolean accelerate) {
        return accelerate ? acceleratedApp.getOrThrow(DataPointManager.class) : sqlApp.getOrThrow(DataPointManager.class);
    }

    @Override
    public Stream<DataPoint> getHistoryData(Symbol symbol, LocalDateTime startTime, LocalDateTime endTime, int numberOfPoints) {
        if (numberOfPoints < 2) {
            throw new IllegalArgumentException("The number of points returned shall always be more than 2");
        }
        long start = startTime.toEpochSecond(ZoneOffset.UTC);
        long end = endTime.toEpochSecond(ZoneOffset.UTC);
        long range = end - start;
        double step = range / (numberOfPoints - 1);  // The number of steps if dividing evenly over given range
        int granularity = Math.min(
            MAXIMUM_GRANULARITY_STEP,
            Math.max(1, (int)step)
        );
        return dataPoints.stream()
            .filter(DataPoint.SYMBOL_ID.equal(symbol.getId()))
            .filter(DataPoint.TIME_STAMP.between(start, end, Inclusion.START_INCLUSIVE_END_INCLUSIVE))
            .sorted(DataPoint.TIME_STAMP)
            .collect(SieveCollector.of(
                DataPoint.TIME_STAMP,
                numberOfPoints,
                granularity
            )).get();
    }

    @Override
    public Optional<DataPoint> getMostRecentDataPoint(Symbol symbol) {
        return dataPoints.stream()
            .filter(DataPoint.SYMBOL_ID.equal(symbol.getId()))
            .sorted(DataPoint.TIME_STAMP.comparator().reversed())
            .findFirst();
    }

    @Override
    public Stream<Symbol> getSymbols() {
        return sqlApp.getOrThrow(SymbolManager.class).stream();
    }

    public static void main(String[] args) {
        final String host = "localhost";
        final String user = "root";
        final String password = "root";
        Service service = Service.create(host, user, password).withAcceleration(true);

        Optional<Symbol> symbolOptional = service.getSymbols().findAny();
        if (symbolOptional.isPresent()) {
            Symbol symbol = symbolOptional.get();
            System.out.println("10 oldest data for " + symbol.getName());
            service.getHistoryData(symbolOptional.get(), LocalDateTime.MIN, LocalDateTime.MAX, 1)
                .limit(10)
                .forEachOrdered(System.out::println);

            System.out.println("Most recent data for all symbols that have data:");
            service.getSymbols()
                .map(service::getMostRecentDataPoint)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(System.out::println);

//            Portfolio portfolio = service.getPortfolio();
//            BigDecimal preValue = portfolio.getCurrentValue();
//            final int amount = 10000;
//            portfolio.addInvestment(symbol, amount);
//            BigDecimal postValue = portfolio.getCurrentValue();
//            System.out.println("Adding " + amount + " of " + symbol.getName() + " increses value from " + preValue + " to " + postValue);
//            portfolio.addInvestment(symbol, -amount);
//            assert preValue.equals(portfolio.getCurrentValue());
        } else {
            System.out.println("No symbols in database");
        }
    }
}
