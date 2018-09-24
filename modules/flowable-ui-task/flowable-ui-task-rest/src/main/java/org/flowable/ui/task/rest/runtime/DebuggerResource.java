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
        assertDebuggerEnabled();
        return debuggerService.getBreakpoints();
    }

    @RequestMapping(value = "/rest/debugger/breakpoints", method = RequestMethod.POST)
    public void addBreakPoints(@RequestBody BreakpointRepresentation breakpointRepresentation) {
        assertDebuggerEnabled();
        debuggerService.addBreakpoint(breakpointRepresentation);
    }

    @RequestMapping(value = "/rest/debugger/breakpoints/{executionId}/continue", method = RequestMethod.PUT)
    public void continueExecution(@PathVariable String executionId) {
        assertDebuggerEnabled();
        debuggerService.continueExecution(executionId);
    }

    @RequestMapping(value = "/rest/debugger/breakpoints", method = RequestMethod.DELETE)
    public void deleteBreakPoints(@RequestBody BreakpointRepresentation breakpointRepresentation) {
        assertDebuggerEnabled();
        debuggerService.removeBreakpoint(breakpointRepresentation);
    }

    @RequestMapping(value = "/rest/debugger/eventlog/{processInstanceId}", method = RequestMethod.GET)
    public List<EventLogEntry> getEventLog(@PathVariable String processInstanceId) {
        assertDebuggerEnabled();
        return debuggerService.getProcessInstanceEventLog(processInstanceId);
    }

    @RequestMapping(value = "/rest/debugger/variables/{executionId}", method = RequestMethod.GET)
    public List<DebuggerRestVariable> getExecutionVariables(@PathVariable String executionId) {
        assertDebuggerEnabled();
        return debuggerService.getExecutionVariables(executionId);
    }

    @RequestMapping(value = "/rest/debugger/executions/{processInstanceId}", method = RequestMethod.GET)
    public List<ExecutionRepresentation> getExecutions(@PathVariable String processInstanceId) {
        assertDebuggerEnabled();
        return debuggerService.getExecutions(processInstanceId);
    }

    @RequestMapping(value = "/rest/debugger/evaluate/expression/{executionId}", method = RequestMethod.POST, produces = "application/text")
    public String evaluateExpression(@PathVariable String executionId, @RequestBody String expression) {
        assertDebuggerEnabled();
        return debuggerService.evaluateExpression(executionId, expression).toString();
    }

    @RequestMapping(value = "/rest/debugger/evaluate/{scriptLanguage}/{executionId}", method = RequestMethod.POST)
    public void evaluateScript(@PathVariable String executionId, @PathVariable String scriptLanguage, @RequestBody String script) {
        assertDebuggerEnabled();
        debuggerService.evaluateScript(executionId, scriptLanguage, script);
    }

    @RequestMapping(value = "/rest/debugger", method = RequestMethod.GET)
    public boolean isDebuggerAllowed() {
        return environment.getProperty("flowable.experimental.debugger.enabled", Boolean.class, false);
    }

    protected void assertDebuggerEnabled() {
        if (!environment.getProperty("flowable.experimental.debugger.enabled", Boolean.class, false)) {
            throw new RuntimeException("property flowable.experimental.debugger.enabled is not enabled");
        }
    }

}
