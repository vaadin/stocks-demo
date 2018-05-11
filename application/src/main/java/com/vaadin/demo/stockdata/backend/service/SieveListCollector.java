package com.vaadin.demo.stockdata.backend.service;

import com.vaadin.demo.stockdata.backend.service.internal.SieveListCollectorImpl;

import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.Stream;

public interface SieveListCollector {

    /**
     * <p>Create a sieving collector that can be used to create an evenly spaced subset of the collected stream.
     * The stream to collect shall be sorted and the result will be sorted.
     *
     * <p>The stream supplied as output of the collection contains elements of the collected stream fulfilling the
     * following criteria;
     *
     * <ol>
     * <li>The first and last items of the stream are preserved as the first and the last items of the collected stream</li>
     * <li>The distance between items is not smaller than <code>minimumDistance</code>.
     *     This has precedence over 3 below but not 1 above.</li>
     * <li>The number of collected items is close to the given <code>size</code>.</li>
     * <li>The collected items are evenly spaced w.r.t <code>distanceMeasure</code></li>
     * </ol>
     *
     * @param distanceMeasure a mapping from items to long, used to determine the distance between items
     * @param size            the requested number of items in the collected output stream
     * @param minimumDistance a lower limit on the distance between items of the collected stream.
     * @param <T>             the type of the items of the streams
     * @throws IllegalArgumentException if the collected stream is not sorted w.r.t distanceMeasure
     * @return a collector that yields a supplier of a stream of an evenly spaced subset of the collected stream
     */
    static <T> Collector<T, ?, Supplier<Stream<T>>> of(ToLongFunction<T> distanceMeasure, int size, int minimumDistance) {
        return new SieveListCollectorImpl<>(distanceMeasure, size, minimumDistance);
    }
}
