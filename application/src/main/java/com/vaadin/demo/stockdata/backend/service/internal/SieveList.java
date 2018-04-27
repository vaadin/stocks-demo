package com.vaadin.demo.stockdata.backend.service.internal;

import com.vaadin.demo.stockdata.backend.service.MonotonicList;

import java.util.*;
import java.util.function.ToLongFunction;

import static java.util.Objects.requireNonNull;

public class SieveList<T> implements MonotonicList<T> {
    private final int size;
    private final List<T> items;
    private final ToLongFunction<T> distanceMeasure;
    private long startPosition;
    private double step;
    private double nextPosition;
    private T last;

    SieveList(ToLongFunction<T> distanceMeasure, int size, double granularity) {
        if (size < 2) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " needs size of at least 2 items");
        }
        if (granularity < 1) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " needs minimum granularity of at least 1");
        }
        this.size = size;
        this.step = granularity;
        this.distanceMeasure = requireNonNull(distanceMeasure);
        items = new ArrayList<>();
        last = null;
    }

    @Override
    public void add(T item) {
        long itemPosition = distanceMeasure.applyAsLong(item);
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

    @Override
    public MonotonicList<T> merge(MonotonicList<T> other) {
        MonotonicList<T> result = new SieveList<>(distanceMeasure, size, step);
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
        int characteristics = Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.ORDERED;
        return Spliterators.spliteratorUnknownSize(iterator(), characteristics);
    }


}
