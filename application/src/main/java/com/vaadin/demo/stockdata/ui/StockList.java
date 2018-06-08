package com.vaadin.demo.stockdata.ui;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.demo.stockdata.backend.service.Service;
import com.vaadin.demo.stockdata.ui.components.StockGrid;
import com.vaadin.demo.stockdata.ui.data.StockItem;
import com.vaadin.demo.stockdata.ui.util.MoneyFormatter;
import com.vaadin.demo.stockdata.ui.util.ServiceDirectory;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
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
  private StockGrid grid;
  private Service service = ServiceDirectory.getServiceInstance();
  private Set<SymbolSelectedListener> listeners = new HashSet<>();


  StockList() {
    setHeight("100%");
    setWidth("400px");
    getThemeList().add("dark");
    addSearchField();
    addGrid();
  }


  private void addSearchField() {
    TextField searchField = new TextField();
    searchField.addClassName("search-field");
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.setPlaceholder("Search by ticker");
    searchField.setValueChangeMode(ValueChangeMode.EAGER);

    // Tell the grid to update based on the new filter
    searchField.addValueChangeListener(event -> {
      this.filter = event.getValue().toLowerCase();
      grid.getDataProvider().refreshAll();
    });

    add(searchField);
  }

  private void addGrid() {
    grid = new StockGrid();

    // Define how we should fetch data as needed from our Speedment backend
    grid.setDataProvider(DataProvider.fromCallbacks(
        dataQuery -> {
          Stream<Symbol> symbolStream = getSymbolStream();
          return symbolStream.skip(dataQuery.getOffset())
              .limit(dataQuery.getLimit())
              .map(this::toStockItem);
        },
        countQuery ->
            (int) getSymbolStream().count()
    ));

    // React to selection events on the grid
    grid.addSelectionListener(event -> event.getFirstSelectedItem().ifPresent(item -> {
      listeners.forEach(l -> l.symbolSelected(item.getSymbol()));
    }));

    add(grid);
  }

  private Stream<Symbol> getSymbolStream() {
    return service.getSymbols()
                .filter(sym -> filter.isEmpty() || sym.getTicker().toLowerCase().contains(filter));
  }

  // Collect all relevant symbol data into a StockItem
  private StockItem toStockItem(Symbol symbol) {
    StockItem stockItem = new StockItem();
    stockItem.setSymbol(symbol);

    List<Double> history = service.getHistoryData(symbol, LocalDateTime.MIN, LocalDateTime.MAX, 10)
        .map(p -> p.getClose() / 100.0)
        .collect(Collectors.toList());
    stockItem.setHistory(history);

    service.getMostRecentDataPoint(symbol).ifPresent(data -> stockItem.setCurrentValue(MoneyFormatter.format(data.getClose())));

    return stockItem;
  }

  void addSelectedListener(SymbolSelectedListener listener) {
    this.listeners.add(listener);
  }

}
