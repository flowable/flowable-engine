package org.flowable.engine.impl.bpmn.behavior;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.variable.service.delegate.VariableScope;
import org.flowable.variable.service.impl.el.ExpressionManager;
import org.springframework.util.StringUtils;

/**
 * @author martin.grofcik
 */
public class SimulationSubProcessActivityBehavior extends SubProcessActivityBehavior {

    public static final String VIRTUAL_PROCESS_ENGINE_VARIABLE_NAME = "_virtualProcessEngine";

    protected String virtualConfigurationResource;

    public SimulationSubProcessActivityBehavior(String virtualConfigurationResource) {
        this.virtualConfigurationResource = virtualConfigurationResource;
    }

    @Override
    public void execute(DelegateExecution execution) {
        ExecutionEntity subProcessExecution = super.createSubProcessExecution(execution);
        ProcessEngine virtualProcessEngine;
        String configuration = virtualConfigurationResourceEvaluated(execution);

        if (StringUtils.hasText(configuration)) {
            virtualProcessEngine = createProcessEngineFromResource(configuration);
        } else {
            virtualProcessEngine = getCurrentProcessEngine();
        }

        subProcessExecution.setVariable(VIRTUAL_PROCESS_ENGINE_VARIABLE_NAME, virtualProcessEngine.getName());
        CommandContextUtil.getAgenda().planContinueProcessOperation(subProcessExecution);
    }

    protected String virtualConfigurationResourceEvaluated(DelegateExecution execution) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();
        return StringUtils.hasText(this.virtualConfigurationResource) ?
            (String) expressionManager.createExpression(this.virtualConfigurationResource).getValue(execution) :
            null;
    }

    protected ProcessEngine getCurrentProcessEngine() {
        return ProcessEngines.getProcessEngine(
                Context.getCommandContext().getCurrentEngineConfiguration().getEngineName()
        );
    }

    protected ProcessEngine createProcessEngineFromResource(String configuration) {

        ProcessEngineConfiguration virtualConfig = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource(
                configuration,
                "virtualProcessEngineConfiguration");
        ProcessEngine virtualProcessEngine = virtualConfig.buildProcessEngine();
        return virtualProcessEngine;
    }

}
