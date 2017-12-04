package com.vaadin.demo.stockdata.ui;

import com.vaadin.demo.stockdata.backend.service.Service;

/**
 * Just use one instance of the service class so we don't run out of memory.
 * FIXME before any public deployments
 */
public class ServiceDirectory {

    private static Service service = null;

    public static Service getServiceInstance(){
        if (service == null) {
            service = Service.create("localhost", "root", "root").withAcceleration(true);
        }

        return service;
    }
}
