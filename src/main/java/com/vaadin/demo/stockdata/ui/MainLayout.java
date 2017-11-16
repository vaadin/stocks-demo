/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.demo.stockdata.ui;

import com.vaadin.router.*;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.html.Div;


@HtmlImport("frontend://styles.html")
@Route("")
public class MainLayout extends Div implements HasUrlParameter<String>, HasDynamicTitle {

    private final SearchField searchField;
    private final StockList stockList;
    private final AccountDetails accountDetails;
    private final StockDetails stockDetails;

    private String currentSymbol = "";

    public MainLayout() {
        addClassName("main-layout");

        searchField = new SearchField();
        stockList = new StockList();
        accountDetails = new AccountDetails();
        stockDetails = new StockDetails();

        add(searchField, stockList, accountDetails, stockDetails);
    }

    @Override
    public void setParameter(BeforeNavigationEvent event, @OptionalParameter String symbol) {
        currentSymbol = symbol;
    }

    @Override
    public String getPageTitle() {
        return currentSymbol != null ? currentSymbol : "Portfolio" + " | Vaadin Stocks";
    }
}
