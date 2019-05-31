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

package org.flowable.engine.impl.bpmn.behavior;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.CaseServiceTask;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmmn.CaseInstanceService;
import org.flowable.engine.impl.delegate.SubProcessActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.EntityLinkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Start a CMMN case with the case service task
 *
 * @author Tijs Rademakers
 */
public class CaseTaskActivityBehavior extends AbstractBpmnActivityBehavior implements SubProcessActivityBehavior {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseTaskActivityBehavior.class);

    private static final long serialVersionUID = 1L;

    @Override
    public void execute(DelegateExecution execution) {

        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        CaseServiceTask caseServiceTask = (CaseServiceTask) executionEntity.getCurrentFlowElement();

        CommandContext commandContext = CommandContextUtil.getCommandContext();

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();
        CaseInstanceService caseInstanceService = processEngineConfiguration.getCaseInstanceService();
        
        if (caseInstanceService == null) {
            throw new FlowableException("To use the case service task a CaseInstanceService implementation needs to be available in the process engine configuration");
        }

        String businessKey = null;
        if (!StringUtils.isEmpty(caseServiceTask.getBusinessKey())) {
            Expression expression = expressionManager.createExpression(caseServiceTask.getBusinessKey());
            businessKey = expression.getValue(execution).toString();

        } else if (caseServiceTask.isInheritBusinessKey()) {
            ExecutionEntity processInstance = executionEntityManager.findById(execution.getProcessInstanceId());
            businessKey = processInstance.getBusinessKey();
        }
        
        String caseInstanceName = null;
        if (StringUtils.isNotEmpty(caseServiceTask.getCaseInstanceName())) {
            Expression caseInstanceNameExpression = expressionManager.createExpression(caseServiceTask.getCaseInstanceName());
            caseInstanceName = caseInstanceNameExpression.getValue(execution).toString();
        }
        
        Map<String, Object> inParameters = new HashMap<>();

        // copy process variables
        for (IOParameter inParameter : caseServiceTask.getInParameters()) {

            Object value = null;
            if (StringUtils.isNotEmpty(inParameter.getSourceExpression())) {
                Expression expression = expressionManager.createExpression(inParameter.getSourceExpression().trim());
                value = expression.getValue(execution);

            } else {
                value = execution.getVariable(inParameter.getSource());
            }

            String variableName = null;
            if (StringUtils.isNotEmpty(inParameter.getTargetExpression())) {
                Expression expression = expressionManager.createExpression(inParameter.getTargetExpression());
                Object variableNameValue = expression.getValue(execution);
                if (variableNameValue != null) {
                    variableName = variableNameValue.toString();
                } else {
                    LOGGER.warn("In parameter target expression {} did not resolve to a variable name, this is most likely a programmatic error",
                        inParameter.getTargetExpression());
                }

            } else if (StringUtils.isNotEmpty(inParameter.getTarget())){
                variableName = inParameter.getTarget();

            }

            inParameters.put(variableName, value);
        }
        
        String caseInstanceId = caseInstanceService.generateNewCaseInstanceId();
        
        if (processEngineConfiguration.isEnableEntityLinks()) {
            EntityLinkUtil.copyExistingEntityLinks(execution.getProcessInstanceId(), caseInstanceId, ScopeTypes.CMMN);
            EntityLinkUtil.createNewEntityLink(execution.getProcessInstanceId(), caseInstanceId, ScopeTypes.CMMN);
        }

        caseInstanceService.startCaseInstanceByKey(caseServiceTask.getCaseDefinitionKey(), caseInstanceId,
                        caseInstanceName, businessKey, execution.getId(), execution.getTenantId(), caseServiceTask.isFallbackToDefaultTenant(), inParameters);
    }
    
    @Override
    public void completing(DelegateExecution execution, DelegateExecution subProcessInstance) throws Exception {
        // not used
    }
    
    @Override
    public void completed(DelegateExecution execution) throws Exception {
        // not used
    }
    
    public void triggerCaseTask(DelegateExecution execution, Map<String, Object> variables) {
        execution.setVariables(variables);
        leave(execution);
    }
}
