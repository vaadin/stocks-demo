package com.vaadin.demo.stockdata.ui;

import com.vaadin.ui.html.Div;
import com.vaadin.ui.icon.Icon;
import com.vaadin.ui.icon.VaadinIcons;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class AccountDetails extends Div {

    public AccountDetails() {
        addClassName("account-details");
        setText("Jane Doe");
        add(new Icon(VaadinIcons.USER));
    }
}
