package com.vaadin.demo.stockdata.backend.service.internal;

import com.vaadin.demo.stockdata.backend.service.MonotonicList;
import com.vaadin.demo.stockdata.backend.service.SieveListCollector;

import java.util.Collections;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SieveListCollectorImpl<T> implements SieveListCollector<T> {
    private final ToLongFunction<T> distanceMeasure;
    private final int size;
    private final long granularity;

    public SieveListCollectorImpl(ToLongFunction<T> distanceMeasure, int size, long granularity) {
        this.distanceMeasure = distanceMeasure;
        this.size = size;
        this.granularity = granularity;
    }

    @Override
    public Supplier<MonotonicList<T>> supplier() {
        return () -> new SieveList<>(distanceMeasure, size, granularity);
    }

    @Override
    public BiConsumer<MonotonicList<T>, T> accumulator() {
        return MonotonicList::add;
    }

    @Override
    public BinaryOperator<MonotonicList<T>> combiner() {
        return MonotonicList::merge;
    }

    @Override
    public Function<MonotonicList<T>, Supplier<Stream<T>>> finisher() {
        return list -> () -> StreamSupport.stream(list.spliterator(), true);
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
