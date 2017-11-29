package com.vaadin.demo.stockdata.backend.setup;

import com.speedment.common.benchmark.Stopwatch;
import com.speedment.common.benchmark.internal.StopwatchImpl;
import com.speedment.runtime.core.Speedment;
import com.speedment.runtime.core.component.transaction.TransactionComponent;
import com.speedment.runtime.core.component.transaction.TransactionHandler;
import com.speedment.runtime.core.stream.parallel.ParallelStrategy;
import com.vaadin.demo.stockdata.backend.db.StockdataApplicationBuilder;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPointManager;
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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

    private void clearAndPopulate() throws IOException {
        final TransactionComponent transactionComponent = app.getOrThrow(TransactionComponent.class);
        final TransactionHandler txHandler = transactionComponent.createTransactionHandler();

        DataPointManager dataPoints = app.getOrThrow(DataPointManager.class);
        final AtomicLong realDataCount = new AtomicLong();
        final AtomicLong doneSymbols = new AtomicLong();
        final AtomicLong startedSymbols = new AtomicLong();

        wipeDatabase();
        final SymbolManager symbols = app.getOrThrow(SymbolManager.class);

        getNadaqSymbols().forEach(symbols::persist);

        final long symbolCount = symbols.stream().count();
        System.out.println("Created " + symbols.stream().count() + " symbols.");

        final Stopwatch sw = new StopwatchImpl().start();
        Consumer<Long> progressConsumer = progress -> {
            final long time = sw.elapsedMillis();
            long pointsPerS = progress * 1000 / time;
            synchronized (this) {
                System.out.format("\r Points: %d (%d real) (%d points/s) <%d/%d/%d>      ",
                    progress,
                    realDataCount.get(),
                    pointsPerS,
                    doneSymbols.get(),
                    startedSymbols.get(),
                    symbolCount);
                System.out.flush();
            }
        };
        final CachingPersister<DataPoint> persister = new CachingPersister<>(txHandler, dataPoints, progressConsumer);
        final AlphaVantageClient stockClient = new AlphaVantageClient();

        System.out.println("Fetching and extrapolating data points for symbols: <done/started/all>");

        app.configure(SymbolManager.class)
            .withParallelStrategy(ParallelStrategy.computeIntensityExtreme())
            .build()
            .stream()
            .parallel()
            .forEach( symbol -> {
                startedSymbols.incrementAndGet();

                List<DataPoint> realData = stockClient.getDataPoints(apiKey, symbol)
                    .sorted(DataPoint.TIME_STAMP.comparator())
                    .collect(Collectors.toList());

                realDataCount.addAndGet(realData.size());
                realData.forEach(persister);

                DataPointExtrapolator.extrapolate(realData, extrapolateStep)
                    .parallel()
                    .forEach(persister);

                doneSymbols.incrementAndGet();

                progressConsumer.accept(persister.getCount());
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
            creator.clearAndPopulate();
        }
    }
}
