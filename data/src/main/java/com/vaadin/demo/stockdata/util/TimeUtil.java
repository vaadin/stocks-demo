package com.vaadin.demo.stockdata.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class TimeUtil {
    private TimeUtil() {
        throw new UnsupportedOperationException();
    }

    public static long dateStringToSecondsSinceEpoch(String strDate) throws ParseException {
        long millis = new SimpleDateFormat("yyyy-MM-dd").parse(strDate).getTime();
        long seconds = millis / 1000;
        return (int) seconds;
    }
}
