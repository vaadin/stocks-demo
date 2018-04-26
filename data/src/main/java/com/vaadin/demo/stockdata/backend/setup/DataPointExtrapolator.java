package com.vaadin.demo.stockdata.backend.setup;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPointImpl;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class DataPointExtrapolator implements Iterator<DataPoint> {
    private final Iterator<DataPoint> realPoints;
    private final Random random;
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
    private DataPoint next;

    static Stream<DataPoint> extrapolate(Stream<DataPoint> givenPoints, long extrapolateStep) {
        DataPointExtrapolator iterator = new DataPointExtrapolator(givenPoints, extrapolateStep);
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false)
                .onClose(givenPoints::close);
    }

    private DataPointExtrapolator(Stream<DataPoint> pointStream, long extrapolateStep) {
        this.realPoints = pointStream.iterator();
        this.extrapolateStep = extrapolateStep;
        if (realPoints.hasNext()) {
            right = realPoints.next();
            random = new Random();
            symbolId = right.getSymbolId();
            timeForNextPoint = right.getTimeStamp();
            advance();
        } else {
            random = null;
            symbolId = 0;
            next = null;  // explicit just to be clear
        }
    }

    private boolean stepRealTime() {
        while (realPoints.hasNext()) {
            left = right;
            right = realPoints.next();
            timeForNextPoint = left.getTimeStamp() + extrapolateStep;
            if (timeForNextPoint < right.getTimeStamp()) {
                timeDelta = right.getTimeStamp() - left.getTimeStamp();
                highDelta = right.getHigh() - left.getHigh();
                lowDelta = right.getLow() - left.getLow();
                lastClose = left.getClose();
                next = createNextDataPoint(timeForNextPoint);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    private void advance() {
        while (timeForNextPoint >= right.getTimeStamp()) {
            if (!stepRealTime()) {
                next = null;
                return;
            }
        }

        next = createNextDataPoint(timeForNextPoint);
        timeForNextPoint += extrapolateStep;
    }

    @Override
    public DataPoint next() {
        DataPoint result = next;
        advance();
        return result;
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
}
