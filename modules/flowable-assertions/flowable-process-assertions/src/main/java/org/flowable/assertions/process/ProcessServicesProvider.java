package org.flowable.assertions.process;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;

public class ProcessServicesProvider {
    final ProcessEngine processEngine;

    private ProcessServicesProvider(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    static ProcessServicesProvider of(ProcessEngine processEngine) {
        return new ProcessServicesProvider(processEngine);
    }

    RuntimeService getRuntimeService() {
        return processEngine.getRuntimeService();
    }

    HistoryService getHistoryService() {
        return processEngine.getHistoryService();
    }
}
