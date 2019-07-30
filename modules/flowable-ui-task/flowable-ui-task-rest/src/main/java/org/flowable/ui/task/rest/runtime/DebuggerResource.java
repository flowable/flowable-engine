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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping(value = "/rest/debugger/breakpoints", produces = "application/json")
    public Collection<BreakpointRepresentation> getBreakpoints() {
        assertDebuggerEnabled();
        return debuggerService.getBreakpoints();
    }

    @PostMapping(value = "/rest/debugger/breakpoints")
    public void addBreakPoints(@RequestBody BreakpointRepresentation breakpointRepresentation) {
        assertDebuggerEnabled();
        debuggerService.addBreakpoint(breakpointRepresentation);
    }

    @PutMapping(value = "/rest/debugger/breakpoints/{executionId}/continue")
    public void continueExecution(@PathVariable String executionId) {
        assertDebuggerEnabled();
        debuggerService.continueExecution(executionId);
    }

    @DeleteMapping(value = "/rest/debugger/breakpoints")
    public void deleteBreakPoints(@RequestBody BreakpointRepresentation breakpointRepresentation) {
        assertDebuggerEnabled();
        debuggerService.removeBreakpoint(breakpointRepresentation);
    }

    @GetMapping(value = "/rest/debugger/eventlog/{processInstanceId}")
    public List<EventLogEntry> getEventLog(@PathVariable String processInstanceId) {
        assertDebuggerEnabled();
        return debuggerService.getProcessInstanceEventLog(processInstanceId);
    }

    @GetMapping(value = "/rest/debugger/variables/{executionId}")
    public List<DebuggerRestVariable> getExecutionVariables(@PathVariable String executionId) {
        assertDebuggerEnabled();
        return debuggerService.getExecutionVariables(executionId);
    }

    @GetMapping(value = "/rest/debugger/executions/{processInstanceId}")
    public List<ExecutionRepresentation> getExecutions(@PathVariable String processInstanceId) {
        assertDebuggerEnabled();
        return debuggerService.getExecutions(processInstanceId);
    }

    @PostMapping(value = "/rest/debugger/evaluate/expression/{executionId}", produces = "application/text")
    public String evaluateExpression(@PathVariable String executionId, @RequestBody String expression) {
        assertDebuggerEnabled();
        return debuggerService.evaluateExpression(executionId, expression).toString();
    }

    @PostMapping(value = "/rest/debugger/evaluate/{scriptLanguage}/{executionId}")
    public void evaluateScript(@PathVariable String executionId, @PathVariable String scriptLanguage, @RequestBody String script) {
        assertDebuggerEnabled();
        debuggerService.evaluateScript(executionId, scriptLanguage, script);
    }

    @GetMapping(value = "/rest/debugger")
    public boolean isDebuggerAllowed() {
        return environment.getProperty("flowable.experimental.debugger.enabled", Boolean.class, false);
    }

    protected void assertDebuggerEnabled() {
        if (!environment.getProperty("flowable.experimental.debugger.enabled", Boolean.class, false)) {
            throw new RuntimeException("property flowable.experimental.debugger.enabled is not enabled");
        }
    }

}
