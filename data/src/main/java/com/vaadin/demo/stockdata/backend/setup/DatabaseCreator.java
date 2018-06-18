package com.vaadin.demo.stockdata.backend.setup;

import com.speedment.common.benchmark.Stopwatch;
import com.speedment.common.benchmark.internal.StopwatchImpl;
import com.speedment.runtime.core.Speedment;
import com.speedment.runtime.core.exception.SpeedmentException;
import com.speedment.runtime.core.manager.Manager;
import com.speedment.runtime.core.stream.parallel.ParallelStrategy;
import com.vaadin.demo.stockdata.backend.db.StockdataApplicationBuilder;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPointManager;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.SymbolImpl;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.SymbolManager;
import com.vaadin.demo.stockdata.backend.setup.AlphaVantageClient.FetchSize;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

public class DatabaseCreator implements AutoCloseable {
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static final String CONNECTION_URL = "jdbc:mysql://%s";
    private static final String CONNECTION_URL_WITH_DB = "jdbc:mysql://%s/stockdata?useSSL=false&useServerPrepStmts=true";
    private static final String SCHEMA_SQL_RESOURCE_NAME = "/Schema.sql";
    private static final String TICKER_LIST_RESOURCE_NAME = "/nasdaq_tickers.csv";
    private static final long SYMBOL_COUNT_LIMIT = 100;  // Long.MAX_VALUE;
    private static final long MAX_PRESENT_WHEN_FETCHING_NEW = 100_000;

    private final String hostIp;
    private final String user;
    private final String password;
    private final String apiKey;
    private final Speedment app;

    public DatabaseCreator(String hostIp, String user, String password, String apiKey) {
        this.hostIp = hostIp;
        this.user = user;
        this.password = password;
        this.apiKey = apiKey;
        this.app = createApp();
    }

    @Override
    public void close() {
        app.close();
    }

    private void createAndPopulate() throws IOException {
        DataPointManager dataPoints = app.getOrThrow(DataPointManager.class);
        final AtomicLong realDataCount = new AtomicLong();
        final AtomicLong doneSymbols = new AtomicLong();
        final AtomicLong startedSymbols = new AtomicLong();

        final SymbolManager symbols = app.getOrThrow(SymbolManager.class);

        try {
            if (symbols.stream().count() < 1) {
                throw new SpeedmentException("No symbols");
            }
        } catch (SpeedmentException e) {
            System.out.println("Unable to load symbols. Recreating database.");
            wipeDatabase();
            System.out.println("Creating symbols...");
            getNadaqSymbols().forEach(symbols::persist);
        }

        final long symbolCount = symbols.stream().count();
        System.out.println("There are " + symbols.stream().count() + " symbols.");

        final Stopwatch sw = new StopwatchImpl().start();
        final AtomicLong addedCounter = new AtomicLong();
        Consumer<Long> progressConsumer = added -> {
            long progress = addedCounter.addAndGet(added);
            final long time = sw.elapsedMillis();
            long pointsPerS = progress * 1000 / time;
            synchronized (this) {
                System.out.format("Added points: %d (%d real) (%d points/s) <%d/%d/%d>%n",
                    progress,
                    realDataCount.get(),
                    pointsPerS,
                    doneSymbols.get(),
                    startedSymbols.get(),
                    symbolCount);
            }
        };

        final AlphaVantageClient stockClient = new AlphaVantageClient();

        final Manager<Symbol> symbolManager = app.configure(SymbolManager.class)
            .withParallelStrategy(ParallelStrategy.computeIntensityExtreme())
            .build();


        for (UpdatePhase phase : UpdatePhase.values()) {
            System.out.println("Update phase " + phase);
            System.out.println("Fetching and extrapolating data for symbols: <done/started/all> ");
            startedSymbols.set(0);
            doneSymbols.set(0);
            Optional<FetchSize> fetchSize = phase.getFetchSize();
            Optional<Long> extrapolateStep = phase.getExtrapolateStep();

            symbolManager.stream()
                .parallel()
                .forEach(symbol -> {
                    startedSymbols.incrementAndGet();

                    long presentCount = dataPoints.stream()
                        .filter(DataPoint.SYMBOL_ID.equal(symbol.getId()))
                        .count();

                    Supplier<Connection> connectionSupplier = () -> {
                        try {
                            return DriverManager.getConnection(
                                String.format(CONNECTION_URL_WITH_DB, hostIp),
                                user,
                                password);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    };

                    try(BatchPointCreator persister = new BatchPointCreator(connectionSupplier, progressConsumer)) {
                        if (presentCount < MAX_PRESENT_WHEN_FETCHING_NEW) {
                            fetchSize.ifPresent(size -> {
                                Set<Long> presentTimeStamps = dataPoints.stream()
                                    .filter(DataPoint.SYMBOL_ID.equal(symbol.getId()))
                                    .map(DataPoint.TIME_STAMP.getter()::apply)
                                    .collect(toSet());

                                List<DataPoint> realData = stockClient.getDataPoints(apiKey, symbol, size)
                                    .filter(point -> !presentTimeStamps.contains(point.getTimeStamp()))
                                    .collect(Collectors.toList());

                                realDataCount.addAndGet(realData.size());
                                realData.forEach(persister);
                            });
                            persister.flush();
                        }
                        Stream<DataPoint> current = dataPoints.stream()
                            .filter(DataPoint.SYMBOL_ID.equal(symbol.getId()))
                            .sorted(DataPoint.TIME_STAMP);

                        extrapolateStep.ifPresent(step -> DataPointExtrapolator.extrapolate(current, step)
                            .forEach(persister)
                        );
                    }

                    doneSymbols.incrementAndGet();

                });
        }

    }

    private void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureDbConnectivity() {
        System.out.print("Trying connection: ");
        final Stopwatch sw = Stopwatch.createStarted();
        sleep(1000); // Heuristic first guess
        for (int i = 0; i < 1000; i++) {
            try {
                System.out.print(".");
                DriverManager.getConnection(
                    String.format(CONNECTION_URL, hostIp),
                    user,
                    password).close();
                System.out.println(" Connected! Took " + sw);
                sleep(100); // Just to make sure we are stable
                System.out.println("DB is stable.");
                return;
            } catch (java.sql.SQLException e) {
                // Ignore
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            sleep(500);
        }
        throw new RuntimeException("Failed to connect to DB");
    }

    private Speedment createApp() {
        ensureDbConnectivity();
        System.out.println("Creating app for DB at " + hostIp);
        return new StockdataApplicationBuilder()
            .withUsername(user)
            .withPassword(password)
            .withIpAddress(hostIp)
            .withSkipLogoPrintout()
            .withAllowStreamIteratorAndSpliterator()
            .build();
    }

    private void wipeDatabase() {
        String url = String.format(CONNECTION_URL, hostIp);
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement())
        {
            System.out.println("Connected to " + url);
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
                .collect(LinkedList::new,
                    (list, line) -> {
                        if (!line.trim().startsWith("--")) {
                            if (list.isEmpty() || list.getLast().trim().endsWith(";")) {
                                list.addLast(line);
                            } else {
                                list.addLast(list.removeLast() + "\n" + line);
                            }
                        }
                    },
                    ($1, $2) -> {throw new IllegalStateException("cannot handle a parallel stream of lines");}
                );
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
                .sorted(Symbol.TICKER)
                .limit(SYMBOL_COUNT_LIMIT)
                .collect(toList());
        }
    }

    private static void usage() {
        System.out.println("Usage: <ip address> <user> <password> <api key>");
        System.out.println("       <ip address>: IP address of MySQL host");
        System.out.println("       <user>      : MySQL user name");
        System.out.println("       <password>  : MySQL password");
        System.out.println("       <api key>   : Alpha Vantage API Key, get it here:");
        System.out.println("                     https://www.alphavantage.co/support/#api-key");
        System.exit(1);
    }

    public static void main(String[] args) throws ClassNotFoundException, IOException {

        if (args.length != 4) {
            String params = Arrays.stream(args).collect(joining("', '"));
            System.out.println("Needs 4 parameters, but got '" + params + "'");
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
        String apiKey = args[3];

        try (final DatabaseCreator creator = new DatabaseCreator(hostIp, user, password, apiKey)) {
            creator.createAndPopulate();
        }
    }
}
