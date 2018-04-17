package org.flowable.ui.task.service.debugger;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessDebugger;
import org.flowable.job.api.Job;
import org.flowable.ui.task.model.debugger.BreakpointRepresentation;
import org.flowable.ui.task.model.debugger.ExecutionRepresentation;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        List<Job> brokenJobs = getManagementService().createSuspendedJobQuery().
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
        Job job = getManagementService().createSuspendedJobQuery().handlerType(HANDLER_TYPE_BREAK_POINT).executionId(executionId).singleResult();
        if (job == null) {
            throw new FlowableException("No broken job found for execution '" + executionId + "'");
        }

        getManagementService().moveSuspendedJobToExecutableJob(job.getId());
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
   
    protected HistoryService getHistoricService() {
        return this.applicationContext.getBean(HistoryService.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public List<DebuggerRestVariable> getExecutionVariables(String executionId) {
        List<Execution> executions = getRuntimeService().createExecutionQuery().executionId(executionId).list();
        if (executions.isEmpty()) {
            return getHistoricService().createHistoricVariableInstanceQuery().executionId(executionId).list().stream().
                    map(DebuggerRestVariable::new).
                    collect(Collectors.toList());
        }
        return getRuntimeService().getVariableInstances(executionId).values().stream().
                map(DebuggerRestVariable::new).
                collect(Collectors.toList());
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
