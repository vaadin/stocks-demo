package com.vaadin.demo.stockdata.backend.service.internal;

import com.speedment.enterprise.datastore.runtime.DataStoreBundle;
import com.speedment.enterprise.datastore.runtime.DataStoreComponent;
import com.speedment.enterprise.datastore.runtime.StreamSupplierComponentDecorator;
import com.speedment.enterprise.datastore.runtime.collector.SieveCollector;
import com.speedment.enterprise.sharding.MutableShardedSpeedment;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

public class ServiceImpl implements Service {

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
    private final MutableShardedSpeedment<Symbol> shardedApp;
    private Function<Symbol, Stream<DataPoint>> dataPointProducer;
    private final String user;
    private final String password;
    private final String hostIp;

    public ServiceImpl(String hostIp, String user, String password) {
        this.hostIp = hostIp;
        this.user = user;
        this.password = password;
        sqlApp = createSqlApp();
        shardedApp = MutableShardedSpeedment.create();
        dataPointProducer = getDataPointSupplier(true);
    }

    private Speedment createSqlApp() {
        StockdataApplicationBuilder builder = new StockdataApplicationBuilder()
            .withUsername(user)
            .withPassword(password)
            .withIpAddress(hostIp)
            .withLogging(ApplicationBuilder.LogType.STREAM);

        return builder.build();
    }

    private Speedment acceleratedApplicationBuilder(Symbol symbol) {
        final StockdataApplicationBuilder builder = new StockdataApplicationBuilder()
            .withUsername(user)
            .withPassword(password)
            .withIpAddress(hostIp)
            .withLogging(ApplicationBuilder.LogType.STREAM)
            .withBundle(VirtualColumnBundle.class)
            .withSkipLogoPrintout()
            .withBundle(DataStoreBundle.class);

        final StockdataApplication application = builder.build();

        DataStoreComponent dataStoreComponent = application.getOrThrow(DataStoreComponent.class);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // A decorator to filter out the data for this particular shard
        final StreamSupplierComponentDecorator streamDecorator = StreamSupplierComponentDecorator.builder()
            .withStreamDecorator(DataPointManager.IDENTIFIER,
                s -> s.filter(DataPoint.SYMBOL_ID.equal(symbol.getId())))
            .build();

        // make first load in current Thread to ensure we have loaded when returning
        dataStoreComponent.load(executor, streamDecorator);

        // Setup periodic reload of the data store
        Executors.newSingleThreadScheduledExecutor()
            .scheduleWithFixedDelay(() ->
                dataStoreComponent.reload(executor, streamDecorator),2, 2, TimeUnit.MINUTES);

        return application;
    }

    private String getLicenseKey() {
        final String licenseKey = System.getenv("SPEEDMENT_LICENSE").trim();
        return licenseKey.trim();
    }

    @Override
    public Service withAcceleration(boolean accelerate) {
        dataPointProducer = getDataPointSupplier(accelerate);
        return this;
    }

    private Function<Symbol, Stream<DataPoint>> getDataPointSupplier(boolean accelerate) {
        return accelerate
            ?
            symbol -> shardedApp
                .computeIfAbsent(symbol, this::acceleratedApplicationBuilder)
                .getOrThrow(DataPointManager.class)
                .stream()
            :
            symbol -> sqlApp.getOrThrow(DataPointManager.class)
                .stream()
                .filter(DataPoint.SYMBOL_ID.equal(symbol.getId()));
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
        return dataPointProducer.apply(symbol)
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
        return dataPointProducer.apply(symbol)
            .sorted(DataPoint.TIME_STAMP.comparator().reversed())
            .findFirst();
    }

    @Override
    public Stream<Symbol> getSymbols() {
        return sqlApp.getOrThrow(SymbolManager.class).stream();
    }
}
