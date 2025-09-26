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
package org.flowable.cmmn.engine.configurator.impl.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.CaseServiceTask;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.engine.impl.process.ProcessInstanceService;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.TriggerCaseTaskCmd;
import org.flowable.engine.impl.persistence.entity.BpmnEngineEntityConstants;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.form.api.FormInfo;

/**
 * @author Joram Barrez
 */
public class DefaultProcessInstanceService implements ProcessInstanceService {

    private static final String DELETE_REASON = "deletedFromCmmnCase";
    
    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public DefaultProcessInstanceService(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public String generateNewProcessInstanceId() {
        if (processEngineConfiguration.isUsePrefixId()) {
            return BpmnEngineEntityConstants.BPMN_ENGINE_ID_PREFIX + processEngineConfiguration.getIdGenerator().getNextId();
        } else {
            return processEngineConfiguration.getIdGenerator().getNextId();
        }
    }

    @Override
    public String startProcessInstanceByKey(String processDefinitionKey, String predefinedProcessInstanceId, String stageInstanceId,
            String tenantId, Boolean fallbackToDefaultTenant, String parentDeploymentId, Map<String, Object> inParametersMap, String businessKey,
            Map<String, Object> variableFormVariables, FormInfo variableFormInfo, String variableFormOutcome) {
        
        return startProcessInstanceByKey(processDefinitionKey, predefinedProcessInstanceId, null, stageInstanceId, tenantId, fallbackToDefaultTenant,
                parentDeploymentId, inParametersMap, businessKey, variableFormVariables, variableFormInfo, variableFormOutcome);
    }

    @Override
    public String startProcessInstanceByKey(String processDefinitionKey, String predefinedProcessInstanceId, String planItemInstanceId, String stageInstanceId,
            String tenantId, Boolean fallbackToDefaultTenant, String parentDeploymentId, Map<String, Object> inParametersMap, String businessKey,
            Map<String, Object> variableFormVariables, FormInfo variableFormInfo, String variableFormOutcome) {
        
        ProcessInstanceBuilder processInstanceBuilder = processEngineConfiguration.getRuntimeService().createProcessInstanceBuilder();
        processInstanceBuilder.processDefinitionKey(processDefinitionKey);
        if (tenantId != null) {
            processInstanceBuilder.tenantId(tenantId);
            processInstanceBuilder.overrideProcessDefinitionTenantId(tenantId);
        }

        if (parentDeploymentId != null) {
            processInstanceBuilder.processDefinitionParentDeploymentId(parentDeploymentId);
        }
        
        processInstanceBuilder.predefineProcessInstanceId(predefinedProcessInstanceId);

        if (planItemInstanceId != null) {
            processInstanceBuilder.callbackId(planItemInstanceId);
            processInstanceBuilder.callbackType(CallbackTypes.PLAN_ITEM_CHILD_PROCESS);
        }

        for (String target : inParametersMap.keySet()) {
            processInstanceBuilder.variable(target, inParametersMap.get(target));
        }

        if (fallbackToDefaultTenant != null && fallbackToDefaultTenant) {
            processInstanceBuilder.fallbackToDefaultTenant();
        }

        if (businessKey != null) {
            processInstanceBuilder.businessKey(businessKey);
        }

        if (stageInstanceId != null) {
            processInstanceBuilder.stageInstanceId(stageInstanceId);
        }

        if (variableFormInfo != null) {
            processInstanceBuilder.formVariables(variableFormVariables, variableFormInfo, variableFormOutcome);
        }

        ProcessInstance processInstance = processInstanceBuilder.start();
        return processInstance.getId();
    }
    

    @Override
    public void triggerCaseTask(String executionId, Map<String, Object> variables) {
        processEngineConfiguration.getCommandExecutor().execute(new TriggerCaseTaskCmd(executionId, variables));
    }
    
    @Override
    public List<IOParameter> getOutputParametersOfCaseTask(String executionId) {
        ExecutionEntity execution = (ExecutionEntity) processEngineConfiguration.getExecutionEntityManager().findById(executionId);
        if (execution == null) {
            throw new FlowableException("No execution could be found for id " + executionId);
        }
        
        FlowElement flowElement = execution.getCurrentFlowElement();
        if (!(flowElement instanceof CaseServiceTask caseServiceTask)) {
            // The execution already processed this stage, there is no need to copy parameters anymore.
            // One possible reason for this is that the case task was terminated by a boundary event.
            return Collections.emptyList();
        }
        
        List<IOParameter> cmmnParameters = new ArrayList<>();

        List<org.flowable.bpmn.model.IOParameter> parameters = caseServiceTask.getOutParameters();
        for (org.flowable.bpmn.model.IOParameter ioParameter : parameters) {
            IOParameter parameter = new IOParameter();
            parameter.setSource(ioParameter.getSource());
            parameter.setSourceExpression(ioParameter.getSourceExpression());
            parameter.setTarget(ioParameter.getTarget());
            parameter.setTargetExpression(ioParameter.getTargetExpression());
            cmmnParameters.add(parameter);
        }
        
        return cmmnParameters;
    }

    @Override
    public void deleteProcessInstance(String processInstanceId) {
        processEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            
            ExecutionEntity processInstanceEntity = CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstanceId);
            if (processInstanceEntity == null || processInstanceEntity.isDeleted()) {
                return null;
            }

            CommandContextUtil.getExecutionEntityManager(commandContext).deleteProcessInstance(processInstanceEntity.getProcessInstanceId(), DELETE_REASON, false);
            
            return null;
        });
    }

    @Override
    public Object getVariable(String executionId, String variableName) {
        return processEngineConfiguration.getRuntimeService().getVariable(executionId, variableName);
    }

    @Override
    public Map<String, Object> getVariables(String executionId){
       return processEngineConfiguration.getRuntimeService().getVariables(executionId);
    }

    @Override
    public Object resolveExpression(String executionId, String expressionString) {
        Expression expression = processEngineConfiguration.getExpressionManager().createExpression(expressionString);
        return processEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            ExecutionEntity executionEntity = CommandContextUtil.getExecutionEntityManager(commandContext).findById(executionId);
            return expression.getValue(executionEntity);
        });
    }

}
