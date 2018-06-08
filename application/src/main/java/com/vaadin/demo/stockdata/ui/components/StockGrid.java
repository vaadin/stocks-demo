package com.vaadin.demo.stockdata.ui.components;

import com.vaadin.demo.stockdata.ui.data.StockItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.renderer.TemplateRenderer;

public class StockGrid extends Grid<StockItem> {

  public StockGrid() {
    setSizeFull();
    setSelectionMode(SelectionMode.SINGLE);
    setPageSize(20);
    getElement().setAttribute("theme", "no-border no-row-borders");

    // Configure columns
    // Render the stock item rows. Using templates instead of full components aid performance, a lot!
    addColumn(TemplateRenderer.<StockItem>of(
        "<small><b>[[item.nasdaq]]</b></small>")
        .withProperty("nasdaq", stockItem -> stockItem.getSymbol().getTicker()))
        .setWidth("55px");

    addColumn(TemplateRenderer.<StockItem>of(
        "<sparkline-chart class$=[[item.trend]]>" +
            "<vaadin-chart-series values=[[item.historicalData]]></vaadin-chart-series>" +
            "</sparkline-chart>")
        .withProperty("historicalData", StockItem::getHistory)
        .withProperty("trend", StockItem::getTrend));

    addColumn(TemplateRenderer.<StockItem>of(
        "<div style='border-radius=5px;background-color: [[item.trendColor]];border-radius: 5px;color: white;font-weight: bold;padding: 5px 10px 5px 10px;'>[[item.price]]</div>")
        .withProperty("price", StockItem::getCurrentValue)
        .withProperty("trendColor", StockItem::getTrendColor))
        .setWidth("65px");
  }
}
