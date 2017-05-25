package org.flowable.rest.service.api.management;

import java.util.Map;

/**
 * @author martin.grofcik
 */
public class EventLogInsertRequest {
    private String type;
    private String processDefinitionId;
    private String processInstanceId;
    private String executionId;
    private String taskId;
    private Map<String, Object> data;

    public String getType() {
        return type;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getTaskId() {
        return taskId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
