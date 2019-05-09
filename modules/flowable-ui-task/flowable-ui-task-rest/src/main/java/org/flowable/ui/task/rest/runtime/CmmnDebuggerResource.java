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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntityImpl;
import org.flowable.ui.task.model.debugger.BreakpointRepresentation;
import org.flowable.ui.task.model.debugger.ExecutionRepresentation;
import org.flowable.ui.task.model.debugger.PlanItemRepresentation;
import org.flowable.ui.task.service.debugger.CmmnDebuggerService;
import org.flowable.ui.task.service.debugger.DebuggerRestVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing a cmmn debugger requests.
 *
 * @author martin.grofcik
 */
@RestController
@RequestMapping("/app")
public class CmmnDebuggerResource {

    @Autowired
    protected CmmnDebuggerService debuggerService;

    @Autowired
    protected Environment environment;

    @RequestMapping(value = "/rest/cmmn-debugger/breakpoints", method = RequestMethod.GET, produces = "application/json")
    public Collection<BreakpointRepresentation> getBreakpoints() {
        assertDebuggerEnabled();
        return debuggerService.getBreakpoints();
    }

    @RequestMapping(value = "/rest/cmmn-debugger/breakpoints", method = RequestMethod.POST)
    public void addBreakPoints(@RequestBody BreakpointRepresentation breakpointRepresentation) {
        assertDebuggerEnabled();
        debuggerService.addBreakpoint(breakpointRepresentation);
    }

    @RequestMapping(value = "/rest/cmmn-debugger/breakpoints/{planItemId}/continue", method = RequestMethod.PUT)
    public void continuePlanItem(@PathVariable String planItemId) {
        assertDebuggerEnabled();
        debuggerService.continuePlanItem(planItemId);
    }

    @RequestMapping(value = "/rest/cmmn-debugger/breakpoints", method = RequestMethod.DELETE)
    public void deleteBreakPoints(@RequestBody BreakpointRepresentation breakpointRepresentation) {
        assertDebuggerEnabled();
        debuggerService.removeBreakpoint(breakpointRepresentation);
    }

    // todo: event log
    @RequestMapping(value = "/rest/cmmn-debugger/eventlog/{caseId}", method = RequestMethod.GET)
    public List<EventLogEntry> getEventLog(@PathVariable String caseId) {
        assertDebuggerEnabled();
        List<EventLogEntry> evenlogEntries = new ArrayList<>();
        EventLogEntryEntityImpl e1 = new EventLogEntryEntityImpl();
        e1.setData("START CASE INSTANCE".getBytes());
        e1.setExecutionId("e0cf19fb-70f8-11e9-a8b9-b46bfcd9c998");
        e1.setLogNumber(1);
        EventLogEntryEntityImpl e2 = new EventLogEntryEntityImpl();
        e2.setData("COMPLETE HUMAN TASK 1".getBytes());
        e2.setExecutionId("e0cf19fb-70f8-11e9-a8b9-b46bfcd9c998");
        e2.setLogNumber(2);
        EventLogEntryEntityImpl e3 = new EventLogEntryEntityImpl();
        e3.setData("COMPLETE HUMAN TASK 2".getBytes());
        e3.setExecutionId("e0cf19fb-70f8-11e9-a8b9-b46bfcd9c998");
        e3.setLogNumber(2);
        evenlogEntries.add(e1);
        evenlogEntries.add(e2);
        evenlogEntries.add(e3);

        return evenlogEntries;
    }

    @RequestMapping(value = "/rest/cmmn-debugger/variables/{planItemId}", method = RequestMethod.GET)
    public List<DebuggerRestVariable> getPlanItemVariables(@PathVariable String planItemId) {
        assertDebuggerEnabled();
        return debuggerService.getPlanItemVariables(planItemId);
    }

    @RequestMapping(value = "/rest/cmmn-debugger/planItems/{caseId}", method = RequestMethod.GET)
    public List<PlanItemRepresentation> getPlanItems(@PathVariable String caseId) {
        assertDebuggerEnabled();
        return debuggerService.getPlanItemInstances(caseId);
    }

    @RequestMapping(value = "/rest/cmmn-debugger/planitems/{caseInstanceId}", method = RequestMethod.GET)
    public Collection<String> getBrokenPlanItems(@PathVariable String caseInstanceId) {
        assertDebuggerEnabled();
        return debuggerService.getBrokenPlanItems(caseInstanceId);
    }

    @RequestMapping(value = "/rest/cmmn-debugger/evaluate/expression/{planItemId}", method = RequestMethod.POST, produces = "application/text")
    public String evaluateExpression(@PathVariable String planItemId, @RequestBody String expression) {
        assertDebuggerEnabled();
        return debuggerService.evaluateExpression(planItemId, expression).toString();
    }

    @RequestMapping(value = "/rest/cmmn-debugger/evaluate/{scriptLanguage}/{planItemId}", method = RequestMethod.POST)
    public void evaluateScript(@PathVariable String planItemId, @PathVariable String scriptLanguage, @RequestBody String script) {
        assertDebuggerEnabled();
        debuggerService.evaluateScript(planItemId, scriptLanguage, script);
    }

    @RequestMapping(value = "/rest/cmmn-debugger", method = RequestMethod.GET)
    public boolean isDebuggerAllowed() {
        return environment.getProperty("flowable.experimental.debugger.enabled", Boolean.class, false);
    }

    protected void assertDebuggerEnabled() {
        if (!environment.getProperty("flowable.experimental.debugger.enabled", Boolean.class, false)) {
            throw new RuntimeException("property flowable.experimental.debugger.enabled is not enabled");
        }
    }

}
