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

import com.vaadin.router.PageTitle;
import com.vaadin.router.Route;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.html.Div;


@Route("")
@HtmlImport("frontend://styles.html")
@PageTitle("Vaadin Stocks")
public class StocksApp extends Div {

    private SearchField searchField;
    private StockList stockList;
    private StockDetails stockDetails;
    private AccountDetails accountDetails;

    public StocksApp() {
        addClassName("stocks-app");
        setupLayout();
        addListeners();
    }

    private void setupLayout() {
        searchField = new SearchField();
        stockList = new StockList();
        accountDetails = new AccountDetails();
        stockDetails = new StockDetails();

        add(searchField, stockList, accountDetails, stockDetails);
    }

    private void addListeners() {
        searchField.addValueChangeListener(change ->
                stockList.filter(change.getValue()));

        stockList.addSelectionListener(evt -> {

            stockDetails.setVisible(evt.getFirstSelectedItem().isPresent());

            evt.getFirstSelectedItem().ifPresent(s ->
                            stockDetails.setSymbol(s.getSymbol()));
            }
        );
    }

}
