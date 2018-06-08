package com.vaadin.demo.stockdata.ui.components;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;

/**
 * Pre-configured chart for our needs
 */
public class StockChart extends Chart {

  public StockChart() {

    setTimeline(true);

    Configuration configuration = getConfiguration();
    configuration.getChart().setType(ChartType.LINE);
    configuration.getTitle().setText(null);

    YAxis yAxis = new YAxis();
    yAxis.setFloor(0);
    Labels label = new Labels();
    label.setFormatter("function() { return '$' + this.value; }");
    yAxis.setLabels(label);

    PlotLine plotLine = new PlotLine();
    plotLine.setValue(2);
    yAxis.setPlotLines(plotLine);
    configuration.addyAxis(yAxis);

    Tooltip tooltip = new Tooltip();
    tooltip.setPointFormat("<span>Stock value</span>: <b>${point.y}</b><br/>");
    tooltip.setValueDecimals(2);
    configuration.setTooltip(tooltip);


    PlotOptionsOhlc plotOptions = new PlotOptionsOhlc();
    plotOptions.setAnimation(false);
    configuration.setPlotOptions(plotOptions);

    RangeSelector rangeSelector = new RangeSelector();
    // Enable this to have a range selector and style with CSS or use a vaadin-date-picker.
    rangeSelector.setEnabled(false);
    configuration.setRangeSelector(rangeSelector);

    Navigator navigator = new Navigator();
    navigator.setAdaptToUpdatedData(false);
    configuration.setNavigator(navigator);
  }
}
