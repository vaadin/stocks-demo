package com.vaadin.demo.stockdata.ui;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.charts.Chart;

// This won't be needed after this week as a better version of it will be in vaadin-charts-flow
@DomEvent("xaxes-extremes-set")
public class XAxisExtremesEvent extends ComponentEvent<Chart> {

    private Double min;
    private Double max;

    /**
     * Creates a new event using the given source and indicator whether the
     * event originated from the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     */
    public XAxisExtremesEvent(Chart source, boolean fromClient,
                              @EventData("event.detail.originalEvent.min") Double min,
                              @EventData("event.detail.originalEvent.max") Double max) {
        super(source, fromClient);
        this.min = min;
        this.max = max;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }
}
