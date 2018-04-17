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
package org.flowable.ui.task.rest.runtime;

import org.flowable.engine.event.EventLogEntry;
import org.flowable.ui.task.model.debugger.BreakpointRepresentation;
import org.flowable.ui.task.model.debugger.ExecutionRepresentation;
import org.flowable.ui.task.service.debugger.DebuggerRestVariable;
import org.flowable.ui.task.service.debugger.DebuggerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

/**
 * REST controller for managing a debugger requests.
 *
 * @author martin.grofcik
 */
@RestController
@RequestMapping("/app")
public class DebuggerResource {

    @Autowired
    protected DebuggerService debuggerService;

    @Autowired
    protected Environment environment;

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

    @RequestMapping(value = "/rest/debugger/breakpoints", method = RequestMethod.DELETE)
    public void deleteBreakPoints(@RequestBody BreakpointRepresentation breakpointRepresentation) {
        debuggerService.removeBreakpoint(breakpointRepresentation);
    }

    @RequestMapping(value = "/rest/debugger/eventlog/{processInstanceId}", method = RequestMethod.GET)
    public List<EventLogEntry> getEventLog(@PathVariable String processInstanceId) {
        return debuggerService.getProcessInstanceEventLog(processInstanceId);
    }

    @RequestMapping(value = "/rest/debugger/variables/{executionId}", method = RequestMethod.GET)
    public List<DebuggerRestVariable> getExecutionVariables(@PathVariable String executionId) {
        return debuggerService.getExecutionVariables(executionId);
    }

    @RequestMapping(value = "/rest/debugger/executions/{processInstanceId}", method = RequestMethod.GET)
    public List<ExecutionRepresentation> getExecutions(@PathVariable String processInstanceId) {
        return debuggerService.getExecutions(processInstanceId);
    }

    @RequestMapping(value = "/rest/debugger", method = RequestMethod.GET)
    public boolean isDebuggerAllowed() {
        return environment.getProperty("flowable.experimental.debugger.enabled", Boolean.class, false);
    }

}
