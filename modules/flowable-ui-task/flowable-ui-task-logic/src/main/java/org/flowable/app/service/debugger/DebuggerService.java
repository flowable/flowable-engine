package org.flowable.app.service.debugger;

import org.flowable.app.model.debugger.BreakPointRepresentation;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.ActivateSuspendedJobCmd;
import org.flowable.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.Job;
import org.flowable.engine.runtime.ProcessDebugger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    protected List<BreakPointRepresentation> breakPoints = new ArrayList<>();
    private ApplicationContext applicationContext;

    public void addBreakPoint(BreakPointRepresentation breakPointRepresentation) {
        assert breakPointRepresentation != null && isNotBlank(breakPointRepresentation.getActivityId());
        this.breakPoints.add(breakPointRepresentation);
    }

    public void removeBreakPoint(BreakPointRepresentation breakPointRepresentation) {
        assert breakPointRepresentation != null && isNotBlank(breakPointRepresentation.getActivityId());
        if (!this.breakPoints.remove(breakPointRepresentation)) {
            throw new FlowableException("BreakPoint is not set on the activityId");
        }
    }

    public Collection<BreakPointRepresentation> getBreakPoints() {
        Collection<BreakPointRepresentation> breakPoints = new ArrayList(this.breakPoints);
        return breakPoints;
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

    public void continueExecution(String executionId) {
        Job job = getManagementService().createSuspendedJobQuery().handlerType(HANDLER_TYPE_BREAK_POINT).executionId(executionId).singleResult();
        if (job == null) {
            throw new FlowableException("No broken job found for execution '" + executionId + "'");
        }
        getCommandExecutor().execute(new ActivateSuspendedJobCmd((SuspendedJobEntity) job));
        getManagementService().executeJob(job.getId());
    }

    @Override
    public boolean isBreakPoint(Execution execution) {
        for (BreakPointRepresentation breakPoint : breakPoints) {
            if (Objects.equals(breakPoint.getActivityId(), execution.getActivityId()))
                if (org.apache.commons.lang3.StringUtils.isBlank(breakPoint.getProcessDefinitionId()))
                    return true;
                if (Objects.equals(breakPoint.getProcessDefinitionId(), ((ExecutionEntity) execution).getProcessDefinitionId())) {
                    return true;
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
    protected CommandExecutor getCommandExecutor() {
        return this.applicationContext.getBean(ProcessEngineConfigurationImpl.class).getCommandExecutor();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
