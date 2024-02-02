package org.flowable.assertions.process;

import org.flowable.engine.*;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author martin.grofcik
 */
public class Utils {

    protected static String getProcessDescription(ProcessInstance actual) {
        return getProcessDescription(actual.getProcessDefinitionKey(), actual.getId());
    }

    protected static String getProcessDescription(HistoricProcessInstance actual) {
        return getProcessDescription(actual.getProcessDefinitionKey(), actual.getId());
    }

    protected static String getProcessDescription(String processDefinitionKey, String id) {
        return "Expected process instance <"+processDefinitionKey+", "+id+">";
    }

    protected static TaskService getTaskService() {
        return getProcessEngine().getTaskService();
    }

    protected static ProcessEngine getProcessEngine() {
        return ProcessEngines.getProcessEngines().get("default");
    }

}
