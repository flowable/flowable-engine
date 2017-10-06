package org.flowable.app.service.runtime;

import org.apache.commons.lang3.StringUtils;
import org.flowable.app.model.debugger.BreakpointRepresentation;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.impl.el.ExpressionManager;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.scripting.ScriptingEngines;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessDebugger;
import org.flowable.job.service.Job;
import org.flowable.variable.service.delegate.VariableScope;
import org.springframework.beans.BeansException;
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

    public void continueExecution(String executionId) {
        Job job = getManagementService().createDeadLetterJobQuery().handlerType(HANDLER_TYPE_BREAK_POINT).executionId(executionId).singleResult();
        if (job == null) {
            throw new FlowableException("No broken job found for execution '" + executionId + "'");
        }

        getManagementService().moveDeadLetterJobToExecutableJob(job.getId(), 3);
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

    public Object evaluateExpression(final String executionId, final String expressionString) {
        final ProcessEngineConfigurationImpl processEngineConfiguration = this.applicationContext.getBean(ProcessEngineConfigurationImpl.class);
        return processEngineConfiguration.getManagementService().executeCommand(new Command<Object>() {
            @Override
            public Object execute(CommandContext commandContext) {
                ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();
                Expression expression = expressionManager.createExpression(expressionString);
                Execution execution = Context.getProcessEngineConfiguration().getExecutionEntityManager().findById(executionId);
                return expression.getValue((VariableScope) execution);
            }
        });
    }

    public void evaluateScript(final String executionId, final String script) {
        getManagementService().executeCommand(new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {
                ScriptingEngines scriptingEngines = Context.getProcessEngineConfiguration().getScriptingEngines();
                Execution execution = Context.getProcessEngineConfiguration().getExecutionEntityManager().findById(executionId);
                scriptingEngines.evaluate(script, "groovy", (ExecutionEntityImpl) execution, false);
                return null;
            }
        }
        );
    }
}
