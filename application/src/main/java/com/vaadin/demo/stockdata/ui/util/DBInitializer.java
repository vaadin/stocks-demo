package com.vaadin.demo.stockdata.ui.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class DBInitializer implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    ServiceDirectory.getServiceInstance();
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {

  }
}
