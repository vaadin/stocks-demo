package com.vaadin.demo.stockdata.ui;

import com.vaadin.demo.stockdata.backend.service.Service;

/**
 * Just use one instance of the service class so we don't run out of memory.
 */
public class ServiceDirectory {
    private static final String MYSQL_HOST_NAME_VARIABLE_NAME = "STOCKS_MYSQL_HOST";
    private static final String DEFAULT_MYSQL_HOST_NAME = "localhost";

    private static Service service = null;

    public static synchronized Service getServiceInstance(){
        if (service == null) {
            String hostName = System.getenv(MYSQL_HOST_NAME_VARIABLE_NAME);
            System.out.println("Speedment using DB at hostName " + hostName);
            if (hostName == null) {
                hostName = DEFAULT_MYSQL_HOST_NAME;
            }
            service = Service.create(hostName, "root", "root").withAcceleration(true);
        }

        return service;
    }
}
