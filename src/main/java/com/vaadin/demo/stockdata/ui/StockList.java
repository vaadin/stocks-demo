package com.vaadin.demo.stockdata.ui;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.demo.stockdata.backend.service.Service;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.TemplateRenderer;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@HtmlImport("frontend://sparkline-chart.html")
public class StockList extends Grid<StockList.StockInfo> {


    private Service service = ServiceDirectory.getServiceInstance();
    private String filter = "";

    public StockList() {
        addClassName("stock-list");

        setupColumns();
        setupDataProvider();
    }


    private void setupColumns() {
        addColumn(stockInfo -> stockInfo.getSymbol().getTicker());
        addColumn(TemplateRenderer.<StockInfo>of("<sparkline-chart values='[[item.values]]'></sparkline-chart>").withProperty("values", StockInfo::getHistory)).setFlexGrow(1);
        addColumn(StockInfo::getCurrentValue);
    }

    private void setupDataProvider() {
        setDataProvider(DataProvider.fromCallbacks(
                dataQuery -> service.getSymbols()
                        .filter(sym -> filter.isEmpty() || sym.getName().toLowerCase().contains(filter))
                        .skip(dataQuery.getOffset())
                        .limit(dataQuery.getLimit())
                        .map(this::toStockInfo),
                countQuery -> 3269)); //FIXME
    }

    private StockInfo toStockInfo(Symbol symbol) {
        StockInfo stockInfo = new StockInfo();
        stockInfo.setSymbol(symbol);

        stockInfo.setHistory(service.getHistoryData(symbol, LocalDate.MIN, LocalDate.MAX)
                .limit(10)
                .map(p -> (int) p.getClose()).collect(Collectors.toList()));

        service.getMostRecentDataPoint(symbol).ifPresent(data -> stockInfo.setCurrentValue(MoneyFormatter.format(data.getClose())));
        return stockInfo;
    }

    public void filter(String filter) {
        //TODO
        this.filter = filter.toLowerCase();
    }


    class StockInfo {
        Symbol symbol;
        List<Integer> history;
        String currentValue;

        public Symbol getSymbol() {
            return symbol;
        }

        public void setSymbol(Symbol symbol) {
            this.symbol = symbol;
        }

        public List<Integer> getHistory() {
            return history;
        }

        public void setHistory(List<Integer> history) {
            this.history = history;
        }

        public String getCurrentValue() {
            return currentValue;
        }

        public void setCurrentValue(String currentValue) {
            this.currentValue = currentValue;
        }
    }

}
