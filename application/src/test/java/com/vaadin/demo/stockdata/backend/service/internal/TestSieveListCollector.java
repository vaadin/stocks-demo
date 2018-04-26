package com.vaadin.demo.stockdata.backend.service.internal;

import com.vaadin.demo.stockdata.backend.service.SieveListCollector;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSieveListCollector {
    @Test
    public void testSingle() {
        Supplier<Stream<Integer>> streamSupplier = IntStream.range(0, 1)
                .boxed()
                .collect(SieveListCollector.of(i -> i, 10, 1));
        List<Integer> result = streamSupplier.get().collect(Collectors.toList());
        assertEquals(0, (int) result.get(0));
        assertEquals(1, result.size());
    }

    @Test
    public void testTwo() {
        Supplier<Stream<Integer>> streamSupplier = IntStream.range(0, 2)
                .boxed()
                .collect(SieveListCollector.of(i -> i, 10, 1));
        List<Integer> result = streamSupplier.get().collect(Collectors.toList());
        assertEquals(0, (int) result.get(0));
        assertEquals(1, (int) result.get(1));
        assertEquals(2, result.size());
    }

    @Test
    public void testTen() {
        Supplier<Stream<Integer>> streamSupplier = IntStream.range(0, 10)
                .boxed()
                .collect(SieveListCollector.of(i -> i, 3, 1));
        List<Integer> result = streamSupplier.get().collect(Collectors.toList());
        assertEquals(0, (int) result.get(0));
        assertEquals(5, (int) result.get(1));
        assertEquals(9, (int) result.get(2));
        assertEquals(3, result.size());
    }


    @Test
    public void testAdd() {
        int n = 10_000;
        int s = 100;
        Supplier<Stream<Integer>> streamSupplier = IntStream.range(0, n)
                .boxed()
                .collect(SieveListCollector.of(i -> i, s, 1));
        List<Integer> result = streamSupplier.get().collect(Collectors.toList());
        assertEquals(0, (int) result.get(0));
        assertTrue(result.size() >= s);
    }

    @Test
    public void testParallelAdd() {
        int n = 1_000_000;
        int s = 99;
        Supplier<Stream<Integer>> streamSupplier = IntStream.range(0, n)
                .boxed()
                .parallel()
                .collect(SieveListCollector.of(i -> i, s, 1));
        List<Integer> result = streamSupplier.get().collect(Collectors.toList());
        assertEquals(0, (int) result.get(0));
        assertTrue(result.size() < s + 1);
        assertTrue(result.size() > s - 10);
        Integer last = null;
        for (Integer i : result) {
            if (last != null) {
                assertTrue(last < i);
            }
            last = i;
        }
    }
}
