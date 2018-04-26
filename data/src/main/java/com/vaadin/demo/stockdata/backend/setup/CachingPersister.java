package com.vaadin.demo.stockdata.backend.setup;

import com.speedment.runtime.core.Speedment;
import com.speedment.runtime.core.component.transaction.TransactionComponent;
import com.speedment.runtime.core.component.transaction.TransactionHandler;
import com.speedment.runtime.core.manager.Manager;
import com.speedment.runtime.core.manager.Persister;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPointManager;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

class CachingPersister<T> implements Consumer<T> {
    private static final int BATCH_SIZE = 1_000;

    private final TransactionHandler txHandler;
    private final Persister<T> persister;
    private final Consumer<Long> progressConsumer;
    private volatile ArrayBlockingQueue<T> cache;
    private final AtomicLong persistCount;

    public CachingPersister(Speedment app, Manager<T> manager, Consumer<Long> progress) {
        txHandler = app.getOrThrow(TransactionComponent.class).createTransactionHandler();
        persister = manager.persister();
        persistCount = new AtomicLong();
        progressConsumer = progress;
        createNewQueue();
    }

    private void createNewQueue() {
        cache = new ArrayBlockingQueue<>(BATCH_SIZE);
    }

    @Override
    public void accept(T entity) {
        if (!cache.offer(entity)) {
            ArrayBlockingQueue<T> fullCache;
            synchronized (this) {
                if (!cache.offer(entity)) {
                    fullCache = cache;
                    createNewQueue();
                } else {  // Someone else solved the problem while we waited for the monitor
                    fullCache = null;
                }
            }
            if (fullCache != null) {
                persist(fullCache);
                accept(entity);
            }
        }
    }

    public void flush() {
        synchronized (this) {
            persist(cache);
            cache.clear();
        }
    }

    public long getCount() {
        return persistCount.get();
    }

    private void persist(ArrayBlockingQueue<T> data) {
        txHandler.createAndAccept(
            tx -> {
                data.forEach(persister);
                tx.commit();
            }
        );
        progressConsumer.accept(persistCount.addAndGet(data.size()));
    }
}
