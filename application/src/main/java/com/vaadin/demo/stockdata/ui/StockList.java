package com.vaadin.demo.stockdata.ui;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.demo.stockdata.backend.service.Service;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcons;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@StyleSheet("frontend://styles/stock-list.css")
public class StockList extends VerticalLayout {
  public interface SymbolSelectedListener {
    void symbolSelected(Symbol symbol);
  }

  private String filter = "";
  private Grid<StockItem> grid;
  private TextField searchField;
  private Service service = ServiceDirectory.getServiceInstance();
  private Set<SymbolSelectedListener> listeners = new HashSet<>();


  StockList() {
    setHeight("100%");
    getThemeList().add("dark");
    addSearchField();
    addGrid();
    setupGridDataProvider();
    setupListeners();
  }


  private void addSearchField() {
    searchField = new TextField();
    searchField.addClassName("search-field");
    searchField.setPrefixComponent(VaadinIcons.SEARCH.create());
    searchField.setPlaceholder("Search by stock");
    searchField.setValueChangeMode(ValueChangeMode.EAGER);

    add(searchField);
  }

  private void addGrid() {
    grid = new Grid<>();
    // Render the stock item rows. Using templates instead of full components aid performance, a lot!
    grid.addColumn(TemplateRenderer.<StockItem>of(
        "<small><b>[[item.nasdaq]]</b></small>")
        .withProperty("nasdaq", stockItem -> stockItem.getSymbol().getTicker()))
        .setWidth("55px");

    grid.addColumn(TemplateRenderer.<StockItem>of(
        "<sparkline-chart class$=[[item.trend]]>" +
            "<vaadin-chart-series values=[[item.historicalData]]></vaadin-chart-series>" +
            "</sparkline-chart>")
        .withProperty("historicalData", StockItem::getHistory)
        .withProperty("trend", StockItem::getTrend));

    grid.addColumn(TemplateRenderer.<StockItem>of(
        "<div style='border-radius=5px;background-color: [[item.trendColor]];border-radius: 5px;color: white;font-weight: bold;padding: 5px 10px 5px 10px;'>[[item.price]]</div>")
        .withProperty("price", StockItem::getCurrentValue)
        .withProperty("trendColor", StockItem::getTrendColor))
        .setWidth("65px");

    grid.setHeight("100%");
    grid.setWidth("370px");
    grid.setSelectionMode(Grid.SelectionMode.SINGLE);
    grid.getElement().setAttribute("theme", "no-border no-row-borders");
    add(grid);
  }


  private void setupGridDataProvider() {
    grid.setPageSize(20);
    grid.setDataProvider(DataProvider.fromCallbacks(
        dataQuery -> {
          long start = System.currentTimeMillis();
          Stream<StockItem> items = service.getSymbols()
              .filter(sym -> filter.isEmpty() || sym.getName().toLowerCase().contains(filter))
              .skip(dataQuery.getOffset())
              .limit(dataQuery.getLimit())
              .map(this::toStockItem);
          System.out.println("Data provider call took " + (System.currentTimeMillis() - start));
          return items;
        },
        countQuery -> (int) service.getSymbols().count()));
  }

  private StockItem toStockItem(Symbol symbol) {
    StockItem stockItem = new StockItem();
    stockItem.setSymbol(symbol);

    // FIXME: get today's symbols
    List<Double> history = service.getHistoryData(symbol, LocalDateTime.MIN, LocalDateTime.MAX, 10)
        .map(p -> p.getClose() / 100.0)
        .collect(Collectors.toList());
    stockItem.setHistory(history);

    service.getMostRecentDataPoint(symbol).ifPresent(data -> stockItem.setCurrentValue(MoneyFormatter.format(data.getClose())));

    return stockItem;
  }

  private void setupListeners() {
    searchField.addValueChangeListener(event -> {
      this.filter = event.getValue().toLowerCase();
      grid.getDataProvider().refreshAll();
    });

    grid.addSelectionListener(event -> event.getFirstSelectedItem().ifPresent(item -> {
      listeners.forEach(l -> l.symbolSelected(item.getSymbol()));
    }));
  }

  void addSelectedListener(SymbolSelectedListener listener) {
    this.listeners.add(listener);
  }


}
