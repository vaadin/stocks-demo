package com.vaadin.demo.stockdata.backend.service.internal;

import com.speedment.enterprise.datastore.runtime.DataStoreBundle;
import com.speedment.enterprise.datastore.runtime.DataStoreComponent;
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
import com.vaadin.demo.stockdata.backend.service.Portfolio;
import com.vaadin.demo.stockdata.backend.service.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static com.helger.commons.CGlobal.SECONDS_PER_DAY;

public class ServiceImpl implements Service {

    private static final String LICENSE_KEY = "HhD32Q3KophEmEYFYXBpLW1hbmFnZXIsZGF0YXN0b3JlLGRiMixtc3NxbCxvcmFjbGUscmVhY3Rvcix2aXJ0dWFsLWNvbHVtbnM7PG7WjvzDDv08zjh1gSR7EzdSBlFloOYuvMGIz1/+gsGClX0w/u9o+5hl6I6lVgz20/HP8K81NGHUUiU+lW/Hf3wM4RJybQ6be9OjgKa86aDwjXGGi8k8J/CzAx/vk7YZMUSXp1pRTxrRoRT7FpkHg0sKH2qM2kTR6tyfph8mC5I=";

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
            application.getOrThrow(DataStoreComponent.class).load();
        }

        return application;
    }



    @Override
    public Service withAcceleration(boolean accelerate) {
        dataPoints = getDataPointManager(accelerate);
        return this;
    }

    private DataPointManager getDataPointManager(boolean accellerate) {
        return accellerate ? acceleratedApp.getOrThrow(DataPointManager.class) : sqlApp.getOrThrow(DataPointManager.class);
    }

    @Override
    public Portfolio getPortfolio() {
        return new PortfolioImpl(this, sqlApp);
    }

    @Override
    public Stream<DataPoint> getHistoryData(Symbol symbol, LocalDate startDate, LocalDate endDate) {
        long start = startDate.toEpochDay() * SECONDS_PER_DAY;
        long end = endDate.toEpochDay() * SECONDS_PER_DAY;
        return dataPoints.stream()
            .filter(DataPoint.SYMBOL_ID.equal(symbol.getId()))
            .filter(DataPoint.TIME_STAMP.between(start, end, Inclusion.START_INCLUSIVE_END_INCLUSIVE))
            .sorted(DataPoint.TIME_STAMP.comparator());
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
            service.getHistoryData(symbolOptional.get(), LocalDate.MIN, LocalDate.MAX)
                .limit(10)
                .forEachOrdered(System.out::println);

            System.out.println("Most recent data for all symbols that have data:");
            service.getSymbols()
                .map(service::getMostRecentDataPoint)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(System.out::println);

            Portfolio portfolio = service.getPortfolio();
            BigDecimal preValue = portfolio.getCurrentValue();
            final int amount = 10000;
            portfolio.addInvestment(symbol, amount);
            BigDecimal postValue = portfolio.getCurrentValue();
            System.out.println("Adding " + amount + " of " + symbol.getName() + " increses value from " + preValue + " to " + postValue);
            portfolio.addInvestment(symbol, -amount);
            assert preValue.equals(portfolio.getCurrentValue());
        } else {
            System.out.println("No symbols in database");
        }
    }
}
