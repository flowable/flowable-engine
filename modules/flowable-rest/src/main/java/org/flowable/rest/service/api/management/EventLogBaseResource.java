package org.flowable.rest.service.api.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.runtime.Execution;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages EventLog entries
 *
 * @author martin.grofcik
 */
public class EventLogBaseResource {

    @Autowired
    protected ManagementService managementService;

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected ObjectMapper objectMapper;

    protected List<EventLogEntryResponse> getQueryResponse(String processInstanceId, boolean includeSubProcesses) {
        assert processInstanceId != null;

        List<EventLogEntry> eventLog = new ArrayList<>();
        eventLog.addAll(this.managementService.getEventLogEntriesByProcessInstanceId(processInstanceId));
        if (includeSubProcesses) {
            List<Execution> processInstances = this.runtimeService.createExecutionQuery().rootProcessInstanceId(processInstanceId).
                     onlyProcessInstanceExecutions().onlySubProcessExecutions().list();
            for (Execution subProcessInstance : processInstances) {
                eventLog.addAll(this.managementService.getEventLogEntriesByProcessInstanceId(subProcessInstance.getId()));
            }
        }

        return this.restResponseFactory.createEventLogResponseList(eventLog);
    }

}
