package com.vaadin.demo.stockdata.ui;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.demo.stockdata.backend.service.Service;
import com.vaadin.flow.component.Text;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.events.XAxesExtremesSetEvent;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingDouble;

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
    List<DataSeriesItem> items = service.getHistoryData(symbol, startDate, endDate, DATA_POINT_BATCH_SIZE)
        .map(dataPoint -> {
          OhlcItem ohlcItem = new OhlcItem();
          ohlcItem.setOpen(dataPoint.getOpen() / 100.0);
          ohlcItem.setHigh(dataPoint.getHigh() / 100.0);
          ohlcItem.setLow(dataPoint.getLow() / 100.0);
          ohlcItem.setClose(dataPoint.getClose() / 100.0);
          ohlcItem.setX(Instant.ofEpochSecond(dataPoint.getTimeStamp()));
          return ohlcItem;
        }).collect(Collectors.toList());
    return items;
  }

  private void addDetailChart(Symbol symbol) {
    if (subscription != null) subscription.dispose();

    Chart chart = new Chart();
    chart.setTimeline(true);

    Configuration configuration = chart.getConfiguration();
    configuration.getChart().setType(ChartType.LINE);
    configuration.getTitle().setText(null);

    YAxis yAxis = new YAxis();
    Labels label = new Labels();
    label.setFormatter("function() { return (this.value > 0 ? ' + ' : '') + this.value + '%'; }");
    yAxis.setLabels(label);

    PlotLine plotLine = new PlotLine();
    plotLine.setValue(2);
    yAxis.setPlotLines(plotLine);
    configuration.addyAxis(yAxis);

    Tooltip tooltip = new Tooltip();
    tooltip.setPointFormat("<span>Stock value</span>: <b>{point.y}</b> ({point.change}%)<br/>");
    tooltip.setValueDecimals(2);
    configuration.setTooltip(tooltip);

    DataSeries dataSeries = new DataSeries();
    dataSeries.setName("Value");
    dataSeries.setData(getSymbolData(symbol, LocalDateTime.MIN, LocalDateTime.MAX));
    configuration.setSeries(dataSeries);


    PlotOptionsOhlc plotOptions = new PlotOptionsOhlc();
    Marker marker = new Marker();
    configuration.setPlotOptions(plotOptions);

    RangeSelector rangeSelector = new RangeSelector();
    // Enable this to have a range selector and style with CSS or use a vaadin-date-picker.
    rangeSelector.setEnabled(false);
    configuration.setRangeSelector(rangeSelector);

    Navigator navigator = new Navigator();
    navigator.setAdaptToUpdatedData(false);
    configuration.setNavigator(navigator);

    chart.setWidth("100%");


    Flowable<XAxesExtremesSetEvent> flow = Flowable.create(emitter ->
            chart.addXAxesExtremesSetListener(emitter::onNext),
        BackpressureStrategy.LATEST);

    subscription = flow.debounce(500, TimeUnit.MILLISECONDS)
        .subscribe(event -> {

          List<DataSeriesItem> zoomedData = getSymbolData(symbol,
              timestampToLocalDateTime(event.getMinimum()),
              timestampToLocalDateTime(event.getMaximum()));
          dataSeries.setData(zoomedData);
          Pair<Number, Number> newMinMax = findMinMax(dataSeries);

          getUI().ifPresent(ui -> ui.access(() -> {
            dataSeries.updateSeries();
            configuration.fireAxesRescaled(yAxis, newMinMax.getLeft(), newMinMax.getRight(), true, true);
          }));


        });


    add(chart);
    expand(chart);
  }

  private LocalDateTime timestampToLocalDateTime(Double jsTimestamp) {
    return Instant.ofEpochMilli(jsTimestamp.longValue()).atZone(ZoneId.systemDefault()).toLocalDateTime();
  }

  private Pair<Number, Number> findMinMax(DataSeries dataSeries) {
    List<DataSeriesItem> data = dataSeries.getData();
    Number min = data.stream().map(DataSeriesItem::getLow).min(comparingDouble(Number::intValue)).orElse(0.0);
    Number max = data.stream().map(DataSeriesItem::getHigh).max(comparingDouble(Number::intValue)).orElse(0.0);
    return Pair.of(Math.max(0, min.intValue() - 15), Math.min(100, max.intValue() + 15));
  }

}
