package com.vaadin.demo.stockdata.ui;

import com.vaadin.addon.charts.model.OhlcItem;
import com.vaadin.addon.charts.model.style.Color;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.generated.GeneratedSymbol;
import com.vaadin.demo.stockdata.backend.service.Service;
import com.vaadin.flow.model.Convert;
import com.vaadin.flow.model.Exclude;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.ClientDelegate;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Tag("stock-details")
@HtmlImport("stock-details.html")
public class StockDetails extends PolymerTemplate<StockDetails.Model> {

    private Service service = ServiceDirectory.getServiceInstance();

    public void setVisible(boolean visible) {
        getElement().setAttribute("hidden", !visible);
    }

    public interface Model extends TemplateModel {
        void setSymbol(GeneratedSymbol symbol);

        void setData(List<List<String>> data);

        void setCurrentValue(String close);
    }


    public void setSymbol(Symbol symbol) {
        service.getMostRecentDataPoint(symbol).ifPresent(data -> {
            getModel().setCurrentValue(MoneyFormatter.format(data.getClose()));
            convertAndSetDataPoints(symbol);
            getModel().setSymbol(symbol);
        });
    }

    private void convertAndSetDataPoints(Symbol symbol) {
        List<List<String>> dataPoints = service.getHistoryData(symbol, LocalDate.MIN, LocalDate.MAX)
                .limit(500)
                .map(point ->
                        Arrays.asList(
                                String.valueOf(point.getTimeStamp() * 1000),
                                String.valueOf(point.getOpen() / 10.0),
                                String.valueOf(point.getHigh() / 10.0),
                                String.valueOf(point.getLow() / 10.0),
                                String.valueOf(point.getClose() / 10.0)))
                .collect(Collectors.toList());
        getModel().setData(dataPoints);
    }


    @ClientDelegate
    public void updateInterval(int start, int end) {
        System.out.println("Start: " + start + " End: " + end);
    }


    public static class OHLCPoint {
        private String open;
        private String high;
        private String low;
        private String close;
        private String timestamp;

        public OHLCPoint(String open, String high, String low, String close, String timestamp) {
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.timestamp = timestamp;
        }

        public String getOpen() {
            return open;
        }

        public void setOpen(String open) {
            this.open = open;
        }

        public String getHigh() {
            return high;
        }

        public void setHigh(String high) {
            this.high = high;
        }

        public String getLow() {
            return low;
        }

        public void setLow(String low) {
            this.low = low;
        }

        public String getClose() {
            return close;
        }

        public void setClose(String close) {
            this.close = close;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }
}
