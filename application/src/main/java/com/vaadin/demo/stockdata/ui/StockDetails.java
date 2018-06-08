package com.vaadin.demo.stockdata.ui;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.demo.stockdata.backend.service.Service;
import com.vaadin.demo.stockdata.ui.components.StockChart;
import com.vaadin.demo.stockdata.ui.util.MoneyFormatter;
import com.vaadin.demo.stockdata.ui.util.ServiceDirectory;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.charts.events.XAxesExtremesSetEvent;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.OhlcItem;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@StyleSheet("frontend://styles/stock-details.css")
public class StockDetails extends VerticalLayout implements StockList.SymbolSelectedListener {

  /**
   * Approximate number of data points returned in each batch
   */
  private static final int DATA_POINT_BATCH_SIZE = 300;

  private Service service = ServiceDirectory.getServiceInstance();
  private Disposable subscription;


  StockDetails() {
    addClassName("stock-details");
    setSizeFull();
    setSpacing(true);

    showNoSymbolSelected();
  }

  private void showNoSymbolSelected() {
    Span noSymbolSelected = new Span("No symbol selected");
    noSymbolSelected.getStyle().set("align-self", "center");
    add(noSymbolSelected);
  }

  @Override
  public void symbolSelected(Symbol symbol) {
    removeAll();

    if (symbol != null) {
      addSymbolDetailsLayout(symbol);
      addDetailChart(symbol);
    } else {
      showNoSymbolSelected();
    }

    // Todo: Remove when https://github.com/vaadin/vaadin-charts-flow/issues/188 gets fixed
    addDetachListener(detach -> {
      if (subscription != null) {
        subscription.dispose();
      }
    });
  }

  private void addSymbolDetailsLayout(Symbol symbol) {
    service.getMostRecentDataPoint(symbol).ifPresent(dataPoint -> {

      Span currentValue = new Span(MoneyFormatter.format(dataPoint.getClose()));
      Div ticker = new Div(new Text(symbol.getTicker()));
      Div name = new Div(new Text(symbol.getName()));
      Div companyInfo = new Div(ticker, name);

      currentValue.addClassName("current-value");
      ticker.addClassName("ticker");
      name.addClassName("name");
      companyInfo.addClassName("company-info");

      FlexLayout flexLayout = new FlexLayout(currentValue, companyInfo);
      flexLayout.setWidth("100%");
      flexLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
      flexLayout.setAlignItems(Alignment.CENTER);
      add(flexLayout);
    });
  }


  private List<DataSeriesItem> getSymbolData(Symbol symbol, LocalDateTime startDate, LocalDateTime endDate) {
    return service.getHistoryData(symbol, startDate, endDate, DATA_POINT_BATCH_SIZE)
        .map(dataPoint -> {
          OhlcItem ohlcItem = new OhlcItem();
          ohlcItem.setOpen(dataPoint.getOpen() / 100.0);
          ohlcItem.setHigh(dataPoint.getHigh() / 100.0);
          ohlcItem.setLow(dataPoint.getLow() / 100.0);
          ohlcItem.setClose(dataPoint.getClose() / 100.0);
          ohlcItem.setX(Instant.ofEpochSecond(dataPoint.getTimeStamp()));
          return ohlcItem;
        }).collect(Collectors.toList());
  }

  private void addDetailChart(Symbol symbol) {
    if (subscription != null) subscription.dispose();

    StockChart chart = new StockChart();

    DataSeries dataSeries = new DataSeries();
    dataSeries.setName("Value");
    dataSeries.setData(getSymbolData(symbol, LocalDateTime.MIN, LocalDateTime.MAX));
    chart.getConfiguration().setSeries(dataSeries);

    // TODO: Remove when https://github.com/vaadin/vaadin-charts-flow/issues/188 gets fixed
    Flowable<XAxesExtremesSetEvent> flow = Flowable.create(emitter ->
            chart.addXAxesExtremesSetListener(emitter::onNext),
        BackpressureStrategy.LATEST);

    subscription = flow.debounce(500, TimeUnit.MILLISECONDS)
        .subscribe(event -> {

          List<DataSeriesItem> zoomedData = getSymbolData(symbol,
              timestampToLocalDateTime(event.getMinimum()),
              timestampToLocalDateTime(event.getMaximum()));
          dataSeries.setData(zoomedData);

          // TODO: remove when the debouncing is done on the client
          getUI().ifPresent(ui -> ui.access(dataSeries::updateSeries));
        });

    add(chart);
    expand(chart);
  }

  private LocalDateTime timestampToLocalDateTime(Double jsTimestamp) {
    return Instant.ofEpochMilli(jsTimestamp.longValue()).atZone(ZoneId.systemDefault()).toLocalDateTime();
  }


}
