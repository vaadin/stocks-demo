package com.vaadin.demo.stockdata.ui;

import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.charts.Chart;

// This won't be needed after this week as a better version of it will be in vaadin-charts-flow
@DomEvent("yaxes-extremes-set")
public class YAxisExtremesEvent extends XAxisExtremesEvent {
    /**
     * Creates a new event using the given source and indicator whether the
     * event originated from the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     * @param min
     * @param max
     */
    public YAxisExtremesEvent(Chart source, boolean fromClient,
                              @EventData("event.detail.originalEvent.min") Double min,
                              @EventData("event.detail.originalEvent.max") Double max) {
        super(source, fromClient, min, max);
    }
}
