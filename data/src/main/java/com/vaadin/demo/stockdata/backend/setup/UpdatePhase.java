package com.vaadin.demo.stockdata.backend.setup;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * The database update goes through a number of phases, each one adding more granularity to the
 * set of data points. This enum encodes the properties of each phase.
 */
enum UpdatePhase {
    SMALL_FETCH(AlphaVantageClient.FetchSize.COMPACT, null),
    LARGE_FETCH(AlphaVantageClient.FetchSize.FULL, TimeUnit.DAYS),
    COARSE_EXTRAPOLATE(null, TimeUnit.HOURS),
    FINE_EXTRAPOLATE(null, TimeUnit.MINUTES),
    FINAL_EXTRAPOLATE(null, TimeUnit.SECONDS);

    /**
     * The size of the batch of data fetched from Alpha Vantage
     */
    private final AlphaVantageClient.FetchSize fetchSize;

    /**
     * The step, in seconds, of data point extrapolation
     */
    private final Long extrapolateStep;

    UpdatePhase(AlphaVantageClient.FetchSize fetchSize, TimeUnit granularity) {
        this.fetchSize = fetchSize;
        this.extrapolateStep = granularity != null ? TimeUnit.SECONDS.convert(1, granularity) : null;
    }

    public Optional<Long> getExtrapolateStep() {
        return Optional.ofNullable(extrapolateStep);
    }

    public Optional<AlphaVantageClient.FetchSize> getFetchSize() {
        return Optional.ofNullable(fetchSize);
    }
}
