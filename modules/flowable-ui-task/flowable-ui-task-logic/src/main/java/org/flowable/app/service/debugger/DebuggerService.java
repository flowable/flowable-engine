package org.flowable.app.service.debugger;

import org.apache.commons.lang3.StringUtils;
import org.flowable.app.model.debugger.BreakpointRepresentation;
import org.flowable.app.model.debugger.ExecutionRepresentation;
import org.flowable.app.model.runtime.RestVariable;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessDebugger;
import org.flowable.job.api.Job;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.flowable.engine.impl.agenda.DebugContinueProcessOperation.HANDLER_TYPE_BREAK_POINT;

/**
 * This class implements basic methods for managing breakpoints
 *
 * @author martin.grofcik
 */
@Service
public class DebuggerService implements ProcessDebugger, ApplicationContextAware {

    protected List<BreakpointRepresentation> breakpoints = new ArrayList<>();
    protected ApplicationContext applicationContext;

    public void addBreakpoint(BreakpointRepresentation breakpointRepresentation) {
        assert breakpointRepresentation != null && isNotBlank(breakpointRepresentation.getActivityId());
        this.breakpoints.add(breakpointRepresentation);
    }

    public void removeBreakpoint(BreakpointRepresentation breakpointRepresentation) {
        assert breakpointRepresentation != null && isNotBlank(breakpointRepresentation.getActivityId());
        if (!this.breakpoints.remove(breakpointRepresentation)) {
            throw new FlowableException("Breakpoint is not set on the activityId");
        }
    }

    public List<BreakpointRepresentation> getBreakpoints() {
        return breakpoints;
    }

    public Collection<String> getBrokenExecutions(String activityId, String processInstanceId) {
        List<Job> brokenJobs = getManagementService().createDeadLetterJobQuery().
                processInstanceId(processInstanceId).
                handlerType(HANDLER_TYPE_BREAK_POINT).
                list();

        ArrayList<String> executions = new ArrayList<>();
        for (Job brokenJob : brokenJobs) {
            Execution brokenJobExecution = getRuntimeService().createExecutionQuery().executionId(brokenJob.getExecutionId()).singleResult();
            if (activityId.equals(brokenJobExecution.getActivityId())) {
                executions.add(brokenJob.getExecutionId());
            }
        }
        return executions;
    }

    public List<EventLogEntry> getProcessInstanceEventLog(String processInstanceId) {
        return getManagementService().getEventLogEntriesByProcessInstanceId(processInstanceId);
    }

    public void continueExecution(String executionId) {
        Job job = getManagementService().createDeadLetterJobQuery().handlerType(HANDLER_TYPE_BREAK_POINT).executionId(executionId).singleResult();
        if (job == null) {
            throw new FlowableException("No broken job found for execution '" + executionId + "'");
        }

        getManagementService().moveDeadLetterJobToExecutableJob(job.getId(), 3);
        try {
            // wait until job is processed
            while (getManagementService().createJobQuery().jobId(job.getId()).count() > 0) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
        }
    }

    @Override
    public boolean isBreakpoint(Execution execution) {
        for (BreakpointRepresentation breakpoint : breakpoints) {
            if (breakpoint.getActivityId().equals(execution.getActivityId())) {
                if (StringUtils.isEmpty(breakpoint.getProcessDefinitionId())) {
                    return true;
                }

                if (Objects.equals(breakpoint.getProcessDefinitionId(), ((ExecutionEntity) execution).getProcessDefinitionId())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected ManagementService getManagementService() {
        return this.applicationContext.getBean(ManagementService.class);
    }

    protected RuntimeService getRuntimeService() {
        return this.applicationContext.getBean(RuntimeService.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public List<RestVariable> getExecutionVariables(String executionId) {
        List<RestVariable> result = new ArrayList<>();
        Map<String, RestVariable> variableMap = new HashMap<>();

        addGlobalVariables(executionId, variableMap);
        addLocalVariables(executionId, variableMap);

        result.addAll(variableMap.values());
        return result;
    }

    protected void addGlobalVariables(String executionId, Map<String, RestVariable> variableMap) {
        try {
            Map<String, Object> rawVariables = getRuntimeService().getVariables(executionId);
            for (String varName : rawVariables.keySet()) {
                RestVariable restVariable = new RestVariable();
                restVariable.setVariableScope(RestVariable.RestVariableScope.GLOBAL);
                restVariable.setName(varName);
                restVariable.setValue(rawVariables.get(varName));
                variableMap.put(varName, restVariable);
            }
        } catch (FlowableObjectNotFoundException e) {
            return;
        }
    }

    protected void addLocalVariables(String executionId, Map<String, RestVariable> variableMap) {
        try {
            Map<String, Object> rawVariables = getRuntimeService().getVariablesLocal(executionId);
            for (String varName : rawVariables.keySet()) {
                RestVariable restVariable = new RestVariable();
                restVariable.setVariableScope(RestVariable.RestVariableScope.LOCAL);
                restVariable.setName(varName);
                restVariable.setValue(rawVariables.get(varName));
                variableMap.put(varName, restVariable);
            }
        } catch (FlowableObjectNotFoundException e) {
            return;
        }
    }

    public List<ExecutionRepresentation> getExecutions(String processInstanceId) {
        List<Execution> executions = getRuntimeService().createExecutionQuery().processInstanceId(processInstanceId).list();
        List<ExecutionRepresentation> executionRepresentations = new ArrayList<>(executions.size());
        for (Execution execution : executions) {
            executionRepresentations.add(new ExecutionRepresentation(execution.getId(), execution.getParentId(), 
                    execution.getProcessInstanceId(), execution.getSuperExecutionId(), execution.getActivityId(), 
                    execution.isSuspended(), execution.getTenantId()));
        }
        return executionRepresentations;
    }
}
