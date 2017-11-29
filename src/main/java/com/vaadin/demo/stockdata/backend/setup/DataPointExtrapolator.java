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
    private final List<DataPoint> realPoints;
    private final Random random;
    private int realPos;
    private int lastRealPos;
    final private int symbolId;
    private DataPoint left;
    private DataPoint right;
    private long timeDelta;
    private long highDelta;
    private long lowDelta;
    private long timeForNextPoint;
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


    private DataPointExtrapolator(int startPos, int endPos, List<DataPoint> realPoints, long extrapolateStep) {
        this.realPoints = realPoints;
        this.extrapolateStep = extrapolateStep;
        this.lastRealPos = endPos;
        random = new Random();
        symbolId = realPoints.get(startPos).getSymbolId();
        realPos = startPos;
        findNextTimePoint();
    }

    private void findNextTimePoint() {
        while (realPos < lastRealPos) {
            left = realPoints.get(realPos);
            right = realPoints.get(realPos + 1);
            timeForNextPoint = left.getTimeStamp() + extrapolateStep;
            if (timeForNextPoint < right.getTimeStamp()) {
                timeDelta = right.getTimeStamp() - left.getTimeStamp();
                highDelta = right.getHigh() - left.getHigh();
                lowDelta = right.getLow() - left.getLow();
                lastClose = left.getClose();
                return;
            } else {
                realPos++;
            }
        }

        // No more real points
        left = realPoints.get(lastRealPos);
        timeForNextPoint = left.getTimeStamp();
        right = left;
    }

    @Override
    public boolean tryAdvance(Consumer<? super DataPoint> action) {
        while (timeForNextPoint >= right.getTimeStamp()) {
            if (realPos < lastRealPos) {
                realPos++;
                findNextTimePoint();
            } else {
                return false;
            }
        }

        action.accept(createNextDataPoint(timeForNextPoint));
        timeForNextPoint += extrapolateStep;
        return true;
    }

    private DataPoint createNextDataPoint(long time) {
        DataPoint next = new DataPointImpl().setSymbolId(symbolId).setTimeStamp(time);
        final float factor = (float)(time - left.getTimeStamp()) / timeDelta;
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
        final int givenLeft = lastRealPos - realPos;
        if (givenLeft > 1) {
            final int mid = realPos + givenLeft / 2;
            final DataPointExtrapolator other = new DataPointExtrapolator(mid, lastRealPos, realPoints, extrapolateStep);
            lastRealPos = mid;
            return other;
        } else {
            return null;
        }
    }

    @Override
    public long estimateSize() {
        return lastRealPos - realPos;
    }

    @Override
    public int characteristics() {
        return NONNULL;
    }
}
