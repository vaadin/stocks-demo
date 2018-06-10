package com.vaadin.demo.stockdata.backend.setup;


import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

class BatchPointCreator implements Consumer<DataPoint>, AutoCloseable {
    private static final int BATCH_SIZE = 10_000;
    public static final int BATCHES_IN_TRANSIT = Runtime.getRuntime().availableProcessors() * 2;
    public static final String INSERT_SQL = "INSERT INTO data_point (symbol_id, time_stamp, open, close, high, low, volume) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private final Consumer<Long> progressConsumer;
    private final ArrayBlockingQueue<DataPoint> buffer;
    private final ArrayBlockingQueue<Collection<DataPoint>> outBound;
    private final ExecutorService executor;
    private final Supplier<Connection> connectionSupplier;

    public BatchPointCreator(Supplier<Connection> connectionSupplier, Consumer<Long> progress) {
        this.connectionSupplier = Objects.requireNonNull(connectionSupplier);
        progressConsumer = Objects.requireNonNull(progress);
        buffer = new ArrayBlockingQueue<>(BATCH_SIZE);
        outBound = new ArrayBlockingQueue<>(BATCHES_IN_TRANSIT);
        executor = Executors.newSingleThreadExecutor(deamonThreadFactory());
    }

    private static ThreadFactory deamonThreadFactory() {
        return task -> {
            final Thread thread = new Thread(task);
            thread.setDaemon(true);
            return thread;
        };
    }

    @Override
    public void accept(DataPoint entity) {
        while (!buffer.offer(entity)) {
            if (!drain()) {
                try {
                    Thread.sleep(1000);  // Back off
                } catch (InterruptedException e) {
                    //
                }
            }
        }
    }

    public void flush() {
        Semaphore done = new Semaphore(0);
        drain();
        executor.execute(done::release);
        done.acquireUninterruptibly();
    }

    private boolean drain() {
        List<DataPoint> batch = new ArrayList<>(BATCH_SIZE);
        buffer.drainTo(batch);
        return spool(batch);
    }

    private boolean spool(Collection<DataPoint> data) {
        try {
            outBound.put(data);
        } catch (InterruptedException e) {
            return false;
        }
        executor.execute(this::persistInTransaction);
        return true;
    }

    private void persistInTransaction() {
        if (!outBound.isEmpty()) {
            try (Connection connection = connectionSupplier.get();
                 PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
                connection.setAutoCommit(false);
                Collection<DataPoint> batch;
                while ((batch = outBound.poll()) != null) {
                    for (DataPoint point : batch) {
                        statement.setInt(1, point.getSymbolId());
                        statement.setLong(2, point.getTimeStamp());
                        statement.setLong(3, point.getOpen());
                        statement.setLong(4, point.getClose());
                        statement.setLong(5, point.getHigh());
                        statement.setLong(6, point.getLow());
                        statement.setLong(7, point.getVolume());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                    connection.commit();
                    progressConsumer.accept((long) batch.size());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        flush();
        executor.shutdown();
    }
}
