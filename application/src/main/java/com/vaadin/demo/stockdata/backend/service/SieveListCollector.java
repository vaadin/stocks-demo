package com.vaadin.demo.stockdata.backend.service;

import com.vaadin.demo.stockdata.backend.service.internal.SieveListCollectorImpl;

import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.Stream;

public interface SieveListCollector<T> extends Collector<T, MonotonicList<T>, Supplier<Stream<T>>> {

    /**
     *
     * @param distanceMeasure
     * @param size
     * @param granularity
     * @param <T>
     * @return
     */
    static <T> SieveListCollector<T> of(ToLongFunction<T> distanceMeasure, int size, long granularity) {
        return new SieveListCollectorImpl<>(distanceMeasure, size, granularity);
    }
}
