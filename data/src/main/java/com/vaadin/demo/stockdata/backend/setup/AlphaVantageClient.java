package com.vaadin.demo.stockdata.backend.setup;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPointImpl;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.demo.stockdata.util.TimeUtil;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.text.ParseException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

class AlphaVantageClient {

    private static final String REST_URI
        = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol=%s&apikey=%s&outputsize=%s";
    private static final String TIME_SERIES_KEY = "Time Series (Daily)";

    public enum FetchSize {
        FULL("full"),
        COMPACT("compact");

        private final String restOutputSize;

        FetchSize(String restOutputSize) {
            this.restOutputSize = restOutputSize;
        }

        public String getRestOutputSize() {
            return restOutputSize;
        }
    }

    private final Client client;

    public AlphaVantageClient() {
        this.client = ClientBuilder.newBuilder().build();
    }

    @SuppressWarnings("unchecked")
    private synchronized Map<String, Map<String, String>> getTimeSeries(String apiKey, Symbol symbol, FetchSize fetchSize) {
        final String uri = String.format(REST_URI, symbol.getTicker(), apiKey, fetchSize.getRestOutputSize());

        int retries = 12;
        Exception lastException;
        do {
            try {
                Map<String, Map> entries = client
                    .target(uri)
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<Map<String, Map>>() {});
                return (Map<String, Map<String, String>>) entries.get(TIME_SERIES_KEY);
            } catch (Exception e) {
                lastException = e;
            }
            try {
                System.out.println("Failed to fetch data for " + symbol.getName() + ", will retry after backing off");
                Thread.sleep(5000);  // Back off, the API does not like to be spammed
            } catch (InterruptedException e) {
                //  Just continue then
            }
        } while (--retries > 0);
        System.out.println();
        System.out.println("Failed to get stock data for " + symbol.getName());
        System.out.println("URI: " + uri);
        lastException.printStackTrace();
        System.out.println();
        return Collections.emptyMap();
    }

    public Stream<DataPoint> getDataPoints(String apiKey, Symbol symbol, FetchSize fetchSize) {
        Map<String, Map<String, String>> map = getTimeSeries(apiKey, symbol, fetchSize);
        return map.entrySet().stream()
            .map(entry -> {
                Map<String, String> pointMap = entry.getValue();
                final DataPointImpl dataPoint = new DataPointImpl();
                dataPoint.setSymbolId(symbol.getId());
                dataPoint.setOpen((int) (Double.valueOf(pointMap.get("1. open")) * 100));
                dataPoint.setHigh((int) (Double.valueOf(pointMap.get("2. high")) * 100));
                dataPoint.setLow((int) (Double.valueOf(pointMap.get("3. low")) * 100));
                dataPoint.setClose((int) (Double.valueOf(pointMap.get("5. adjusted close")) * 100));
                dataPoint.setVolume(Long.valueOf(pointMap.get("6. volume")));
                try {
                    dataPoint.setTimeStamp(TimeUtil.dateStringToSecondsSinceEpoch(entry.getKey()));
                    return (DataPoint) dataPoint;
                } catch (ParseException e) {
                    System.out.println("Skipping data with broken date");
                    e.printStackTrace();
                    return null;
                }
            })
            .filter(Objects::nonNull);
    }

}
