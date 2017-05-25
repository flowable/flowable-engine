package org.flowable.rest.service.api.management;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Manages EventLog entries
 *
 * @author martin.grofcik
 */
@RestController
@Api(tags = {"EventLog"}, description = "Manage EventLog", authorizations = {@Authorization(value = "basicAuth")})
public class EventLogResource extends EventLogBaseResource {

    @ApiOperation(value = "Query for EventLog entities", tags = {
            "EventLog"}, notes = "All supported JSON parameter fields allows filtering by process instance id.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the log entries are returned"),
            @ApiResponse(code = 400, message = "Indicates an parameter was passed in the wrong format. The status-message contains additional information")})
    @RequestMapping(value = "/management/event-log/{processInstanceId}", method = RequestMethod.GET, produces = "application/json")
    public List<EventLogEntryResponse> queryEventLogEntries(@ApiParam(name = "processInstanceId") @PathVariable String processInstanceId, HttpServletRequest request) {
        return getQueryResponse(processInstanceId);
    }

}
