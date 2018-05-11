package com.vaadin.demo.stockdata.backend.service.internal;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

public class SieveListCollectorImpl<T> implements Collector<T, SieveListCollectorImpl.SieveList<T>, Supplier<Stream<T>>> {
    private final ToLongFunction<T> distanceMeasure;
    private final int size;
    private final int granularity;

    public SieveListCollectorImpl(ToLongFunction<T> distanceMeasure, int size, int minimumDistance) {
        this.distanceMeasure = requireNonNull(distanceMeasure);
        if (size < 2) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " needs size of at least 2 items");
        }
        if (minimumDistance < 1) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " needs minimum granularity of at least 1");
        }
        this.size = size;
        this.granularity = minimumDistance;
    }

    @Override
    public Supplier<SieveListCollectorImpl.SieveList<T>> supplier() {
        return () -> new SieveList<>(distanceMeasure, size, granularity);
    }

    @Override
    public BiConsumer<SieveListCollectorImpl.SieveList<T>, T> accumulator() {
        return SieveListCollectorImpl.SieveList::add;
    }

    @Override
    public BinaryOperator<SieveListCollectorImpl.SieveList<T>> combiner() {
        return SieveListCollectorImpl.SieveList::merge;
    }

    @Override
    public Function<SieveListCollectorImpl.SieveList<T>, Supplier<Stream<T>>> finisher() {
        return list -> () -> StreamSupport.stream(list.spliterator(), true);
    }

    @Override
    public Set<Collector.Characteristics> characteristics() {
        return Collections.emptySet();
    }

    static class SieveList<T> implements Iterable<T> {
        private static final int SPLITERATOR_CHARACTERISTICS =
                Spliterator.DISTINCT |
                Spliterator.IMMUTABLE |
                Spliterator.NONNULL |
                Spliterator.ORDERED;

        private final ToLongFunction<T> distanceMeasure;
        private final int size;
        private final List<T> items;
        private long startPosition;
        private double step;
        private double nextPosition;
        private T last;

        SieveList(ToLongFunction<T> distanceMeasure, int size, int step) {
            this.distanceMeasure = distanceMeasure;
            this.size = size;
            this.step = step;
            items = new ArrayList<>();
            last = null;
        }

        void add(T item) {
            long itemPosition = distanceMeasure.applyAsLong(item);
            if (last != null && itemPosition < distanceMeasure.applyAsLong(last)) {
                throw new IllegalArgumentException("Incoming stream has to be sorted");
            }
            if (last == null) {
                items.add(item);
                nextPosition = (double) itemPosition + step;
                startPosition = itemPosition;
            } else {
                if (itemPosition >= nextPosition) {
                    items.add(item);
                    nextPosition = (double) itemPosition + step;
                    if (items.size() % size == 0) {
                        step = (nextPosition - startPosition) / (size - 1);
                    }
                }
            }
            last = item;
        }

        SieveList merge(SieveList<T> other) {
            SieveList<T> result = new SieveList<>(distanceMeasure, size, (int)step);
            Iterator<T> i1 = iterator();
            Iterator<T> i2 = other.iterator();
            T item1 = i1.hasNext() ? i1.next() : null;
            T item2 = i2.hasNext() ? i2.next() : null;
            while (item1 != null || item2 != null) {
                if (item2 == null ||
                        (item1 != null &&
                                distanceMeasure.applyAsLong(item2) >
                                        distanceMeasure.applyAsLong(item1))) {
                    result.add(item1);
                    item1 = i1.hasNext() ? i1.next() : null;
                } else {
                    result.add(item2);
                    item2 = i2.hasNext() ? i2.next() : null;
                }
            }
            return result;
        }

        @Override
        public Iterator<T> iterator() {
            if (items.isEmpty()) {
                return Collections.emptyIterator();
            }
            final T first = items.get(0);
            final long startPosition = distanceMeasure.applyAsLong(first);
            double distance = distanceMeasure.applyAsLong(last) - startPosition;
            final double step = distance / (double)(size-1);
            return new Iterator<T>() {
                private Iterator<T> itemIterator = items.iterator();
                private T current = nextStored();
                private double position = startPosition + step;

                private T nextStored() {
                    if (itemIterator.hasNext()) {
                        return itemIterator.next();
                    }
                    return null;
                }

                private T advance() {
                    T candidate = nextStored();
                    while (candidate != null) {
                        if (distanceMeasure.applyAsLong(candidate) >= position) {
                            position += step;
                            return candidate;
                        }
                        candidate = nextStored();
                    }
                    if (current != last) {
                        return last;
                    }
                    return null;
                }

                @Override
                public boolean hasNext() {
                    return current != null;
                }

                @Override
                public T next() {
                    T result = current;
                    current = advance();
                    return result;
                }
            };
        }

        @Override
        public Spliterator<T> spliterator() {
            return Spliterators.spliteratorUnknownSize(iterator(), SPLITERATOR_CHARACTERISTICS);
        }
    }
}
