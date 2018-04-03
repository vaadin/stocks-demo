package com.vaadin.demo.stockdata.ui;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.PageConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@Route("")
@Theme(Lumo.class)
@HtmlImport("frontend://src/sparkline-chart.html")
@BodySize(height = "100vh", width = "100vw")
@Viewport("width=device-width, minimum-scale=1, initial-scale=1, user-scalable=yes")
public class MainView extends HorizontalLayout {

  public MainView() {
    setSizeFull();
    setSpacing(true);

    StockList stockList = new StockList();
    StockDetails stockDetails = new StockDetails();
    stockList.addSelectedListener(stockDetails);

    add(stockList, stockDetails);
  }

}
