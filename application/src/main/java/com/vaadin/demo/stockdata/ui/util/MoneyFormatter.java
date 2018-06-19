package com.vaadin.demo.stockdata.ui.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class MoneyFormatter {

    public static String format(long cents) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(cents / 100);
    }
}


