package com.vaadin.demo.stockdata.backend.setup;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPointImpl;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

class AlphaVantageClient {

    private static final String REST_URI
        = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol=%s&apikey=%s&outputsize=%s";
    private static final String TIME_SERIES_KEY = "Time Series (Daily)";

    public enum Size {
        FULL("full"),
        COMPACT("compact");

        private final String restOutputSize;

        Size(String restOutputSize) {
            this.restOutputSize = restOutputSize;
        }

        public String getRestOutputSize() {
            return restOutputSize;
        }
    }

    private Client client = ClientBuilder.newClient();

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, String>> getTimeSeries(String apiKey, Symbol symbol, Size size) {
        final String uri = String.format(REST_URI, symbol.getTicker(), apiKey, size.getRestOutputSize());

        int retries = 12;
        Exception lastException;
        do {
            try {
                Map<String, Map> entries = client
                    .target(uri)
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<Map<String, Map>>() {
                    });
                return (Map<String, Map<String, String>>) entries.get(TIME_SERIES_KEY);
            } catch (ResponseProcessingException | ServiceUnavailableException e) {
                lastException = e;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                //  Just continue then
            }
        } while (--retries > 0);
        System.out.println();
        System.out.printf("Failed to get stock data for " + symbol.getName());
        System.out.printf("URI: " + uri);
        lastException.printStackTrace();
        System.out.println();
        return Collections.emptyMap();
    }

    Stream<DataPoint> getDataPoints(String apiKey, Symbol symbol) {
        // return getDataPoints(apiKey, symbol, Size.COMPACT);  //  For a smaller database
        return getDataPoints(apiKey, symbol, Size.FULL);
    }

    private Stream<DataPoint> getDataPoints(String apiKey, Symbol symbol, Size size) {
        Map<String, Map<String, String>> map = getTimeSeries(apiKey, symbol, size);
        return map.entrySet().stream()
            .flatMap(entry -> {
                Map<String, String> DataPoint = entry.getValue();
                final DataPointImpl info = new DataPointImpl();
                info.setSymbolId(symbol.getId());
                info.setOpen((int) (Float.valueOf(DataPoint.get("1. open")) * 100));
                info.setHigh((int) (Float.valueOf(DataPoint.get("2. high")) * 100));
                info.setLow((int) (Float.valueOf(DataPoint.get("3. low")) * 100));
                info.setClose((int) (Float.valueOf(DataPoint.get("5. adjusted close")) * 100));
                info.setVolume(Integer.valueOf(DataPoint.get("6. volume")));
                try {
                    info.setTimeStamp(dateStringToSecondsSinceEpoch(entry.getKey()));
                    return Stream.of(info);
                } catch (ParseException e) {
                    System.out.println("Skipping data with broken date");
                    e.printStackTrace();
                    return Stream.empty();
                }
            });
    }

    private static int dateStringToSecondsSinceEpoch(String strDate) throws ParseException {
        long millis = new SimpleDateFormat("yyyy-MM-dd").parse(strDate).getTime();
        long seconds = millis / 1000;
        return (int) seconds;
    }
}
