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
package org.flowable.cmmn.engine.impl.behavior.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.process.ProcessInstanceService;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.EntityLinkUtil;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.Process;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class ProcessTaskActivityBehavior extends ChildTaskActivityBehavior implements PlanItemActivityBehavior {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessTaskActivityBehavior.class);

    protected Process process;
    protected Expression processRefExpression;
    protected String processRef;
    protected List<IOParameter> inParameters;
    protected List<IOParameter> outParameters;
    protected Boolean fallbackToDefaultTenant;

    public ProcessTaskActivityBehavior(Process process, Expression processRefExpression, ProcessTask processTask) {
        super(processTask.isBlocking(), processTask.getBlockingExpression());
        this.process = process;
        this.processRefExpression = processRefExpression;
        this.processRef = processTask.getProcessRef();
        this.inParameters = processTask.getInParameters();
        this.outParameters = processTask.getOutParameters();
        this.fallbackToDefaultTenant = processTask.getFallbackToDefaultTenant();
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, Map<String, Object> variables) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        ProcessInstanceService processInstanceService = cmmnEngineConfiguration.getProcessInstanceService();
        if (processInstanceService == null) {
            throw new FlowableException("Could not start process instance: no " + ProcessInstanceService.class + " implementation found");
        }

        String externalRef = null;
        if (process != null) {
            externalRef = process.getExternalRef();
        } else if (processRefExpression != null) {
            externalRef = processRefExpression.getValue(planItemInstanceEntity).toString();
        } else if (processRef != null){
            externalRef = processRef;
        }
        if (StringUtils.isEmpty(externalRef)) {
            throw new FlowableException("Could not start process instance: no externalRef defined");
        }

        Map<String, Object> inParametersMap = new HashMap<>();
        handleInParameters(planItemInstanceEntity, cmmnEngineConfiguration, inParametersMap);

        if (variables != null && !variables.isEmpty()) {
            inParametersMap.putAll(variables);
        }

        String processInstanceId = processInstanceService.generateNewProcessInstanceId();
        planItemInstanceEntity.setReferenceType(CallbackTypes.PLAN_ITEM_CHILD_PROCESS);
        planItemInstanceEntity.setReferenceId(processInstanceId);
        
        if (CommandContextUtil.getCmmnEngineConfiguration(commandContext).isEnableEntityLinks()) {
            EntityLinkUtil.copyExistingEntityLinks(planItemInstanceEntity.getCaseInstanceId(), processInstanceId, ScopeTypes.BPMN);
            EntityLinkUtil.createNewEntityLink(planItemInstanceEntity.getCaseInstanceId(), processInstanceId, ScopeTypes.BPMN);
        }
        
        boolean blocking = evaluateIsBlocking(planItemInstanceEntity);
        if (blocking) {
            processInstanceService.startProcessInstanceByKey(externalRef, processInstanceId, planItemInstanceEntity.getId(),
                planItemInstanceEntity.getTenantId(), fallbackToDefaultTenant, inParametersMap);
        } else {
            processInstanceService.startProcessInstanceByKey(externalRef, processInstanceId,
                planItemInstanceEntity.getTenantId(), fallbackToDefaultTenant, inParametersMap);
        }

        if (!blocking) {
            CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstanceEntity);
        }
    }

    protected void handleInParameters(PlanItemInstanceEntity planItemInstanceEntity,
            CmmnEngineConfiguration cmmnEngineConfiguration, Map<String, Object> inParametersMap) {

        if (inParameters == null) {
            return;
        }

        for (IOParameter inParameter : inParameters) {

            String variableName = null;
            if (StringUtils.isNotEmpty(inParameter.getTargetExpression())) {
                Expression expression = cmmnEngineConfiguration.getExpressionManager().createExpression(inParameter.getTargetExpression());
                Object variableNameValue = expression.getValue(planItemInstanceEntity);
                if (variableNameValue != null) {
                    variableName = variableNameValue.toString();
                } else {
                    LOGGER.warn("In parameter target expression {} did not resolve to a variable name, this is most likely a programmatic error",
                        inParameter.getTargetExpression());
                }

            } else if (StringUtils.isNotEmpty(inParameter.getTarget())){
                variableName = inParameter.getTarget();

            }

            Object variableValue = null;
            if (StringUtils.isNotEmpty(inParameter.getSourceExpression())) {
                Expression expression = cmmnEngineConfiguration.getExpressionManager().createExpression(inParameter.getSourceExpression());
                variableValue = expression.getValue(planItemInstanceEntity);

            } else if (StringUtils.isNotEmpty(inParameter.getSource())) {
                variableValue = planItemInstanceEntity.getVariable(inParameter.getSource());

            }

            if (variableName != null) {
                inParametersMap.put(variableName, variableValue);
            }

        }
    }

    @Override
    public void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstance) {
        if (!PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState())) {
            throw new FlowableException("Can only trigger a plan item that is in the ACTIVE state");
        }
        if (planItemInstance.getReferenceId() == null) {
            throw new FlowableException("Cannot trigger process task plan item instance : no reference id set");
        }
        if (!CallbackTypes.PLAN_ITEM_CHILD_PROCESS.equals(planItemInstance.getReferenceType())) {
            throw new FlowableException("Cannot trigger process task plan item instance : reference type '"
                    + planItemInstance.getReferenceType() + "' not supported");
        }


        // Triggering the plan item (as opposed to a regular complete) terminates the process instance
        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstance);
        deleteProcessInstance(commandContext, planItemInstance);
    }

    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {
        if (PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState())) {
            // The process task plan item will be deleted by the regular TerminatePlanItemOperation
            if (PlanItemTransition.TERMINATE.equals(transition) || PlanItemTransition.EXIT.equals(transition)) {
                deleteProcessInstance(commandContext, planItemInstance);
            } else if (PlanItemTransition.COMPLETE.equals(transition)) {

                CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
                ProcessInstanceService processInstanceService = cmmnEngineConfiguration.getProcessInstanceService();

                CaseInstanceEntity caseInstance = cmmnEngineConfiguration.getCaseInstanceEntityManager().findById(planItemInstance.getCaseInstanceId());
                handleOutParameters(planItemInstance, cmmnEngineConfiguration, processInstanceService, caseInstance);

            }
        }
    }

    protected void handleOutParameters(DelegatePlanItemInstance planItemInstance, CmmnEngineConfiguration cmmnEngineConfiguration,
        ProcessInstanceService processInstanceService, CaseInstanceEntity caseInstance) {

        if (outParameters == null) {
            return;
        }

        for (IOParameter outParameter : outParameters) {

            String variableName = null;
            if (StringUtils.isNotEmpty(outParameter.getTarget()))  {
                variableName = outParameter.getTarget();

            } else if (StringUtils.isNotEmpty(outParameter.getTargetExpression())) {
                Object variableNameValue = processInstanceService.resolveExpression(planItemInstance.getReferenceId(), outParameter.getTargetExpression());
                if (variableNameValue != null) {
                    variableName = variableNameValue.toString();
                } else {
                    LOGGER.warn("Out parameter target expression {} did not resolve to a variable name, this is most likely a programmatic error",
                        outParameter.getTargetExpression());
                }

            }

            Object variableValue = null;
            if (StringUtils.isNotEmpty(outParameter.getSourceExpression())) {
                variableValue = processInstanceService.resolveExpression(planItemInstance.getReferenceId(), outParameter.getSourceExpression());

            } else if (StringUtils.isNotEmpty(outParameter.getSource())) {
                variableValue = processInstanceService.getVariable(planItemInstance.getReferenceId(), outParameter.getSource());

            }
            caseInstance.setVariable(variableName, variableValue);
        }
    }

    protected void deleteProcessInstance(CommandContext commandContext, DelegatePlanItemInstance planItemInstance) {
        ProcessInstanceService processInstanceService = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getProcessInstanceService();
        processInstanceService.deleteProcessInstance(planItemInstance.getReferenceId());
    }

}
