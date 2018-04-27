package com.vaadin.demo.stockdata.backend.service;

/**
 * A list that can not shrink
 *
 * @param <T> the type of the items of the list
 */
public interface MonotonicList<T> extends Iterable<T> {

    /**
     * Adds a new item to the list
     * @param item the item to add
     */
    void add(T item);

    /**
     * Returns a new list that is the combination of this list and the given argument. The semantics of the combination
     * operation is defined by the implementing class.
     *
     * @param other the list with wich this list shall be combined
     * @return the combination of this list and the given list
     */
    MonotonicList<T> merge(MonotonicList<T> other);
}
