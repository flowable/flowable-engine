package org.flowable.rest.service;

import org.flowable.engine.ProcessEngines;

public class ProcessEnginesRest extends ProcessEngines {

    public static synchronized void init() {
        isInitialized = true;
    }
}
