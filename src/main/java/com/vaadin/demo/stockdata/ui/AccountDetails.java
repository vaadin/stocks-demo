package com.vaadin.demo.stockdata.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcons;

public class AccountDetails extends Div {

    public AccountDetails() {
        addClassName("account-details");
        setText("Jane Doe");
        add(new Icon(VaadinIcons.USER));
    }
}
