package org.flowable.rest.service.api.management;

import org.flowable.engine.ManagementService;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
    protected RestResponseFactory restResponseFactory;

    protected List<EventLogEntryResponse> getQueryResponse(String processInstanceId) {
        assert processInstanceId != null;

        return this.restResponseFactory.createEventLogResponseList(
                this.managementService.getEventLogEntriesByProcessInstanceId(processInstanceId)
        );
    }

}
