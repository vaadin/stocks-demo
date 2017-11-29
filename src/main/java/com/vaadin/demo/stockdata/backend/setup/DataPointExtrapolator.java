package com.vaadin.demo.stockdata.backend.setup;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPointImpl;

import java.util.List;
import java.util.Random;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class DataPointExtrapolator implements Spliterator<DataPoint> {
    private final List<DataPoint> givenPoints;
    private final Random random;
    private int lastPos;
    private int symbolId;
    private DataPoint left;
    private DataPoint right;
    private long timeDelta;
    private float highDelta;
    private float lowDelta;
    private long timeForNextPoint;
    private int nextGivenPos;
    final private long extrapolateStep;
    private double trend;
    private long lastClose;

    static Stream<DataPoint> extrapolate(List<DataPoint> givenPoints, long extrapolateStep) {
        if (givenPoints.size() < 2) {
            return Stream.empty();
        } else {
            return StreamSupport.stream(
                new DataPointExtrapolator(0,
                    givenPoints.size() - 1,
                    givenPoints,
                    extrapolateStep),
                true);
        }
    }


    private DataPointExtrapolator(int startPos, int lastPos, List<DataPoint> givenPoints, long extrapolateStep) {
        this.givenPoints = givenPoints;
        this.extrapolateStep = extrapolateStep;
        computeDeltas(givenPoints.get(startPos), givenPoints.get(startPos + 1));
        nextGivenPos = startPos + 2;
        this.lastPos = lastPos;
        symbolId = left.getSymbolId();
        random = new Random();
    }

    private void computeDeltas(DataPoint left, DataPoint right) {
        this.left = left;
        this.right = right;
        timeDelta = right.getTimeStamp() - left.getTimeStamp();
        highDelta = (right.getHigh() - left.getHigh());
        lowDelta = (right.getLow() - left.getLow());
        lastClose = left.getClose();
        timeForNextPoint = left.getTimeStamp() + extrapolateStep;
    }

    @Override
    public boolean tryAdvance(Consumer<? super DataPoint> action) {
        while (timeForNextPoint >= right.getTimeStamp()) {
            if (nextGivenPos <= lastPos) {
                computeDeltas(right, givenPoints.get(nextGivenPos++));
            } else {
                return false;
            }
        }

        action.accept(createNextDataPoint((float)(timeForNextPoint - left.getTimeStamp()) / timeDelta));
        timeForNextPoint += extrapolateStep;
        return true;
    }

    private DataPoint createNextDataPoint(float factor) {
        DataPoint next = new DataPointImpl().setSymbolId(symbolId).setTimeStamp(timeForNextPoint);
        next.setHigh(left.getHigh() + (long) (highDelta * factor));
        next.setLow(left.getLow() + (long) (lowDelta * factor));
        trend += random.nextFloat() - 0.5;
        lastClose += trend;
        if (lastClose > next.getHigh()) {
            lastClose = next.getHigh();
            trend = -1;
        } else if (lastClose < next.getLow()) {
            lastClose = next.getLow();
            trend = 1;
        }
        next.setClose(lastClose);
        next.setOpen(lastClose);
        next.setVolume(0);
        return next;
    }

    @Override
    public Spliterator<DataPoint> trySplit() {
        if (lastPos - nextGivenPos > 1) {
            final int mid = (lastPos - nextGivenPos) / 2 + nextGivenPos;
            final DataPointExtrapolator other = new DataPointExtrapolator(mid, lastPos, givenPoints, extrapolateStep);
            lastPos = mid;
            return other;
        } else {
            return null;
        }
    }

    @Override
    public long estimateSize() {
        return lastPos - nextGivenPos;
    }

    @Override
    public int characteristics() {
        return NONNULL;
    }
}
