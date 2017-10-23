/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.app.rest.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.app.model.debugger.BreakpointRepresentation;
import org.flowable.app.service.runtime.DebuggerService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.event.logger.handler.AbstractDatabaseEventLoggerEventHandler;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;

/**
 * REST controller for managing a debugger requests.
 *
 * @author martin.grofcik
 */
@RestController
public class DebuggerResource {

    @Autowired
    protected DebuggerService debuggerService;

    @Autowired
    protected ManagementService managementService;

    @Autowired
    ObjectMapper objectMapper;

    @RequestMapping(value = "/rest/debugger/breakpoints", method = RequestMethod.GET, produces = "application/json")
    public Collection<BreakpointRepresentation> getBreakpoints() {
        return debuggerService.getBreakpoints();
    }

    @RequestMapping(value = "/rest/debugger/breakpoints", method = RequestMethod.POST)
    public void addBreakPoints(@RequestBody BreakpointRepresentation breakpointRepresentation) {
        debuggerService.addBreakpoint(breakpointRepresentation);
    }

    @RequestMapping(value = "/rest/debugger/breakpoints/{executionId}/continue", method = RequestMethod.PUT)
    public void continueExecution(@PathVariable String executionId) {
        debuggerService.continueExecution(executionId);
    }

    @RequestMapping(value = "/rest/debugger/breakpoints/delete", method = RequestMethod.POST)
    public void deleteBreakPoints(@RequestBody BreakpointRepresentation breakpointRepresentation) {
        debuggerService.removeBreakpoint(breakpointRepresentation);
    }

    @RequestMapping(value = "/rest/debugger/evaluate/expression/{executionId}", method = RequestMethod.POST, produces = "application/json")
    public Object evaluateExpression(@PathVariable String executionId, @RequestBody String expression) {
        return debuggerService.evaluateExpression(executionId, expression);
    }

    @RequestMapping(value = "/rest/debugger/evaluate/script/{executionId}", method = RequestMethod.POST)
    public void evaluateScript(@PathVariable String executionId, @RequestBody String script) {
        debuggerService.evaluateScript(executionId, script);
    }

    @RequestMapping(value = "/rest/debugger/event-log", method = RequestMethod.PUT)
    public void insertEventLogEntry(@RequestBody EventLogInsertRequest eventLogInsertRequest, HttpServletRequest request) {
        insertEventLogEntry(eventLogInsertRequest);
    }

    protected void insertEventLogEntry(final EventLogInsertRequest eventLogInsertRequest) {
        this.managementService.executeCommand(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {
                ExternalEventHandler externalEventHandler = new ExternalEventHandler(
                        eventLogInsertRequest.getType(),
                        eventLogInsertRequest.getProcessDefinitionId(),
                        eventLogInsertRequest.getProcessInstanceId(),
                        eventLogInsertRequest.getExecutionId(),
                        eventLogInsertRequest.getTaskId(),
                        eventLogInsertRequest.getData()
                );
                externalEventHandler.setTimeStamp(commandContext.getCurrentEngineConfiguration().getClock().getCurrentTime());
                externalEventHandler.setObjectMapper(objectMapper);
                EventLogEntryEntity eventLogEntryEntity = externalEventHandler.generateEventLogEntry(commandContext);
                CommandContextUtil.getEventLogEntryEntityManager(commandContext).insert(eventLogEntryEntity);
                return null;
            }
        });
    }

    private class ExternalEventHandler extends AbstractDatabaseEventLoggerEventHandler {
        private Map<String, Object> data;
        private String type;
        private String processDefinitionId;
        private String processInstanceId;
        private String executionId;

        public ExternalEventHandler(String type, String processDefinitionId, String processInstanceId, String executionId, String taskId, Map<String, Object> data) {
            this.data = data;
            this.type = type;
            this.processDefinitionId = processDefinitionId;
            this.processInstanceId = processInstanceId;
            this.executionId = executionId;
            this.taskId = taskId;
        }

        private String taskId;

        @Override
        public EventLogEntryEntity generateEventLogEntry(CommandContext commandContext) {
            return createEventLogEntry(this.type, this.processDefinitionId, this.processInstanceId, this.executionId,
                    this.taskId, this.data);
        }
    }

}
