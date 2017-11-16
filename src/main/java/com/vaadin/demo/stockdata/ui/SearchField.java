package com.vaadin.demo.stockdata.ui;

import com.vaadin.ui.icon.Icon;
import com.vaadin.ui.icon.VaadinIcons;
import com.vaadin.ui.textfield.TextField;

public class SearchField extends TextField {

    public SearchField() {
        addClassName("search-field");
        setPlaceholder("Search by stock");

        Icon icon = new Icon(VaadinIcons.SEARCH);
        icon.getElement().setAttribute("slot", "prefix");
        getElement().appendChild(icon.getElement());
    }
}
