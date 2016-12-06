package org.activiti.rest.service;

import org.flowable.engine.ProcessEngines;

public class ProcessEnginesRest extends ProcessEngines {

  public synchronized static void init() {
    isInitialized = true;
  }
}
