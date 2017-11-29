package com.vaadin.demo.stockdata.backend.setup;

import com.speedment.common.benchmark.Stopwatch;
import com.speedment.common.benchmark.internal.StopwatchImpl;
import com.speedment.runtime.core.Speedment;
import com.speedment.runtime.core.component.transaction.TransactionComponent;
import com.speedment.runtime.core.component.transaction.TransactionHandler;
import com.speedment.runtime.core.exception.SpeedmentException;
import com.speedment.runtime.core.stream.parallel.ParallelStrategy;
import com.vaadin.demo.stockdata.backend.db.StockdataApplicationBuilder;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPointManager;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.investmentdata.InvestmentDataImpl;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.investmentdata.InvestmentDataManager;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.SymbolImpl;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.SymbolManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class DatabaseCreator implements AutoCloseable {
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static final String CONNECTION_URL = "jdbc:mysql://%s";
    private static final String SCHEMA_SQL_RESOURCE_NAME = "/Schema.sql";
    private static final String TICKER_LIST_RESOURCE_NAME = "/nasdaq_tickers.csv";

    private final String hostIp;
    private final String user;
    private final String password;
    private final String apiKey;
    private final Speedment app;
    private long extrapolateStep;

    public DatabaseCreator(String hostIp, String user, String password, long extrapolateSecondStep, String apiKey) {
        this.hostIp = hostIp;
        this.user = user;
        this.password = password;
        this.extrapolateStep = extrapolateSecondStep;
        this.apiKey = apiKey;
        this.app = createApp();
    }

    @Override
    public void close() {
        app.stop();
    }

    private void setupDatabase() throws IOException {
        DataPointManager dataPoints = app.getOrThrow(DataPointManager.class);
        try {
            final long count = dataPoints.stream().count();
            System.out.println("Found " + count + " data points in database");
            if (count == 0) {
                System.out.println("Fetching real data points");
                populateDatabaseRealData();
            }
            try {
                extrapolateDataPoints();
            } catch (SpeedmentException e) {
                e.printStackTrace();
            }
            System.out.println("Database now has " + dataPoints.stream().count() + " data points");
        } catch (SpeedmentException e) {
            System.out.println("Database schema missing or malformed, recreating");
            wipeDatabase();
            setupDatabase();
        }
    }

    private void extrapolateDataPoints() {
        final TransactionComponent transactionComponent = app.getOrThrow(TransactionComponent.class);
        final TransactionHandler txHandler = transactionComponent.createTransactionHandler();

        final DataPointManager dataPoints = app.getOrThrow(DataPointManager.class);

        final Stopwatch sw = new StopwatchImpl().start();

        long symbolCount = app.getOrThrow(SymbolManager.class).stream().count();
        AtomicLong doneSymbols = new AtomicLong();
        AtomicLong startedSymbols = new AtomicLong();

        System.out.println("Extrapolating data points <done/started/all>");

        Consumer<Long> progressConsumer = progress -> {
            final long time = sw.elapsedMillis();
            long pointsPerS = progress * 1000 / time;
            synchronized (this) {
                System.out.format("\r Points: %d (%d points/s) <%d/%d/%d>",
                    progress,
                    pointsPerS,
                    doneSymbols.get(),
                    startedSymbols.get(),
                    symbolCount);
                System.out.flush();
            }
        };
        CachingPersister<DataPoint> persister = new CachingPersister<>(txHandler, dataPoints, progressConsumer);

        app.configure(SymbolManager.class)
            .withParallelStrategy(ParallelStrategy.computeIntensityExtreme())
            .build()
            .stream()
            .parallel()
            .forEach(symbol -> {
                final List<DataPoint> existingPoints = dataPoints.stream()
                    .filter(DataPoint.SYMBOL_ID.equal(symbol.getId()))
                    .sorted(Comparator.comparing(DataPoint::getTimeStamp))
                    .collect(toList());

                startedSymbols.incrementAndGet();
                DataPointExtrapolator.extrapolate(existingPoints, extrapolateStep)
                    .parallel()
                    .forEach(persister);
                doneSymbols.incrementAndGet();

                progressConsumer.accept(persister.getCount());
            });

        persister.flush();
        System.out.println();
    }

    private void populateDatabaseRealData() throws IOException {
        final TransactionComponent transactionComponent = app.getOrThrow(TransactionComponent.class);
        final TransactionHandler txHandler = transactionComponent.createTransactionHandler();

        SymbolManager symbols = app.getOrThrow(SymbolManager.class);
        InvestmentDataManager investments = app.getOrThrow(InvestmentDataManager.class);
        DataPointManager dataPoints = app.getOrThrow(DataPointManager.class);

        //getNadaqSymbols().stream().skip(100).limit(10).forEach(symbols::persist);  //  For a smaller database

        getNadaqSymbols().forEach(symbols::persist);
        System.out.println("Created " + symbols.stream().count() + " symbols.");

        investments.persist(new InvestmentDataImpl().setQuantity(1000).setSymbolId(1));
        investments.persist(new InvestmentDataImpl().setQuantity(2000).setSymbolId(2));

        AlphaVantageClient stockClient = new AlphaVantageClient();

        Consumer<Long> progressConsumer = progress -> {
            synchronized (app) {
                System.out.println("Stored a total of " + progress + " data points.");
            }
        };
        CachingPersister<DataPoint> persister = new CachingPersister<>(txHandler, dataPoints, progressConsumer);

        app.configure(SymbolManager.class)
            .withParallelStrategy(ParallelStrategy.computeIntensityExtreme())
            .build()
            .stream()
            .parallel()
            .forEach( symbol -> {
                synchronized (app) {
                    System.out.println("Fetching trade info for " + symbol.getName());
                }
                AtomicLong count = new AtomicLong();
                stockClient.getDataPoints(apiKey, symbol)
                    .peek($ -> count.incrementAndGet())
                    .forEach(persister);
                synchronized (app) {
                    System.out.println("Fetched " + count.get() + " data points for " + symbol.getName());
                }
            });

        persister.flush();
    }

    private Speedment createApp() {
        return new StockdataApplicationBuilder()
            .withUsername(user)
            .withPassword(password)
            .withIpAddress(hostIp)
            .withSkipLogoPrintout()
            .build();
    }

    private void wipeDatabase() {
        String url = String.format(CONNECTION_URL, hostIp);
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement())
        {
            for (String sql : getSqlStatements()) {
                System.out.println("--- Executing SQL Statement");
                System.out.println(sql.trim());
                stmt.executeUpdate(sql);
                System.out.println("---> SQL Statement Executed OK");
                System.out.println();
            }
        } catch (SQLException se) {
            System.out.format("Unable to connect to database, URL: %s, user: %s%n", url, user);
            se.printStackTrace();
        } catch (IOException e) {
            System.out.println("Unable to read database schema resource");
        }
    }

    private List<String> getSqlStatements() throws IOException {
        try (final InputStream in = DatabaseCreator.class.getResourceAsStream(SCHEMA_SQL_RESOURCE_NAME);
             final InputStreamReader reader = new InputStreamReader(in);
             final BufferedReader buf = new BufferedReader(reader))
        {
            return buf.lines()
                .collect(LinkedList::new, (list, line) -> {
                    if (!line.trim().startsWith("--")) {
                        if (list.isEmpty() || list.getLast().trim().endsWith(";")) {
                            list.addLast(line);
                        } else {
                            list.addLast(list.removeLast() + "\n" + line);
                        }
                    }
                }, LinkedList::addAll);
        }
    }

    private static Pattern LINE_PATTERN = Pattern.compile("\"([^\"]+)\",\"([^\"]+)\".*$");

    private List<Symbol> getNadaqSymbols() throws IOException {
        try (final InputStream in = DatabaseCreator.class.getResourceAsStream(TICKER_LIST_RESOURCE_NAME);
             final InputStreamReader reader = new InputStreamReader(in);
             final BufferedReader buf = new BufferedReader(reader))
        {
            return buf.lines()
                .skip(1)  // the header of the csv file
                .flatMap(line -> {
                    Matcher matcher = LINE_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String ticker = matcher.group(1).trim();
                        String name = matcher.group(2).trim();
                        return Stream.of(new SymbolImpl().setTicker(ticker).setName(name));
                    }
                    return Stream.empty();
                })
                .collect(toList());
        }
    }


    private static void usage() {
        System.out.println("Usage: <ip address> <user> <password> <interval> <api key>");
        System.out.println("       <ip address>: IP address of MySQL host");
        System.out.println("       <user>      : MySQL user name");
        System.out.println("       <password>  : MySQL password");
        System.out.println("       <interval>  : Seconds between extrapolated trading info,");
        System.out.println("                     a value of x means that there will be x seconds");
        System.out.println("                     between each extrapolated data point.");
        System.out.println("       <api key>   : Alpha Vantage API Key, get it here:");
        System.out.println("                     https://www.alphavantage.co/support/#api-key");
        System.exit(1);
    }

    public static void main(String[] args) throws ClassNotFoundException, IOException {

        if (args.length != 5) {
            usage();
        }

        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            System.out.println("Missing JDBC driver: " + JDBC_DRIVER);
            throw e;
        }

        String hostIp = args[0];
        String user = args[1];
        String password = args[2];
        String apiKey = args[4];

        long interval = -1;
        try {
            interval = Integer.valueOf(args[3]);
        } catch (NumberFormatException e) {
            // ignore - show usage in next step
        }

        if (interval < 0) {
            usage();
        }

        try (final DatabaseCreator creator = new DatabaseCreator(hostIp, user, password, interval, apiKey)) {
            creator.setupDatabase();
        }
    }
}
