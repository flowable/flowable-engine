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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceBuilderImpl;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class CaseTaskActivityBehavior extends ChildTaskActivityBehavior implements PlanItemActivityBehavior {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseTaskActivityBehavior.class);

    protected Expression caseRefExpression;
    protected Boolean fallbackToDefaultTenant;

    public CaseTaskActivityBehavior(Expression caseRefExpression, CaseTask caseTask) {
        super(caseTask.isBlocking(), caseTask.getBlockingExpression(), caseTask.getInParameters(), caseTask.getOutParameters());
        this.caseRefExpression = caseRefExpression;
        this.fallbackToDefaultTenant = caseTask.getFallbackToDefaultTenant();
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, Map<String, Object> variables) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        CaseInstanceHelper caseInstanceHelper = CommandContextUtil.getCaseInstanceHelper(commandContext);
        CaseInstanceBuilder caseInstanceBuilder = new CaseInstanceBuilderImpl().
                caseDefinitionKey(caseRefExpression.getValue(planItemInstanceEntity).toString());
        if (StringUtils.isNotEmpty(planItemInstanceEntity.getTenantId())) {
            caseInstanceBuilder.tenantId(planItemInstanceEntity.getTenantId());
            caseInstanceBuilder.overrideCaseDefinitionTenantId(planItemInstanceEntity.getTenantId());
        }

        caseInstanceBuilder.parentId(planItemInstanceEntity.getCaseInstanceId());

        if (fallbackToDefaultTenant != null && fallbackToDefaultTenant) {
            caseInstanceBuilder.fallbackToDefaultTenant();
        }

        Map<String, Object> finalVariableMap = new HashMap<>();
        handleInParameters(planItemInstanceEntity, cmmnEngineConfiguration, finalVariableMap, cmmnEngineConfiguration.getExpressionManager());

        if (variables != null && !variables.isEmpty()) {
            finalVariableMap.putAll(variables);
        }

        caseInstanceBuilder.variables(finalVariableMap);
        caseInstanceBuilder.callbackType(CallbackTypes.PLAN_ITEM_CHILD_CASE);
        caseInstanceBuilder.callbackId(planItemInstanceEntity.getId());

        CaseInstanceEntity caseInstanceEntity = caseInstanceHelper.startCaseInstance(caseInstanceBuilder);

        // Bidirectional storing of reference to avoid queries later on
        planItemInstanceEntity.setReferenceType(CallbackTypes.PLAN_ITEM_CHILD_CASE);
        planItemInstanceEntity.setReferenceId(caseInstanceEntity.getId());

        if (!evaluateIsBlocking(planItemInstanceEntity)) {
            CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation((PlanItemInstanceEntity) planItemInstanceEntity);
        }
    }

    @Override
    public void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstance) {
        if (!PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState())) {
            throw new FlowableException("Can only trigger a plan item that is in the ACTIVE state");
        }
        if (planItemInstance.getReferenceId() == null) {
            throw new FlowableException("Cannot trigger case task plan item instance : no reference id set");
        }
        if (!CallbackTypes.PLAN_ITEM_CHILD_CASE.equals(planItemInstance.getReferenceType())) {
            throw new FlowableException("Cannot trigger case task plan item instance : reference type '"
                    + planItemInstance.getReferenceType() + "' not supported");
        }

        // Triggering the plan item (as opposed to a regular complete) terminates the case instance
        CommandContextUtil.getAgenda(commandContext).planManualTerminateCaseInstanceOperation(planItemInstance.getReferenceId());
        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstance);
    }

    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {
        if (PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState())) {
            if (PlanItemTransition.TERMINATE.equals(transition) || PlanItemTransition.EXIT.equals(transition)) {
                // The plan item will be deleted by the regular TerminatePlanItemOperation
                CommandContextUtil.getAgenda(commandContext).planManualTerminateCaseInstanceOperation(planItemInstance.getReferenceId());

            } else if (PlanItemTransition.COMPLETE.equals(transition)) {
                CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
                CaseInstanceEntityManager caseInstanceEntityManager = cmmnEngineConfiguration.getCaseInstanceEntityManager();
                CaseInstanceEntity caseInstance = caseInstanceEntityManager.findById(planItemInstance.getCaseInstanceId());
                handleOutParameters(
                    planItemInstance,
                    caseInstance,
                    cmmnEngineConfiguration.getCmmnRuntimeService(),
                    cmmnEngineConfiguration);
            }

        }
    }

    protected Object resolveExpression(CmmnEngineConfiguration cmmnEngineConfiguration, String executionId, String expressionString) {
        CaseInstanceEntityManager caseInstanceEntityManager = cmmnEngineConfiguration.getCaseInstanceEntityManager();
        Expression expression = cmmnEngineConfiguration.getExpressionManager().createExpression(expressionString);
        CaseInstanceEntity caseInstanceEntity = caseInstanceEntityManager.findById(executionId);
        return expression.getValue(caseInstanceEntity);
    }

    @Override
    public void deleteChildEntity(CommandContext commandContext, DelegatePlanItemInstance delegatePlanItemInstance, boolean cascade) {
        if (CallbackTypes.PLAN_ITEM_CHILD_CASE.equals(delegatePlanItemInstance.getReferenceType())) {
            CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
            CaseInstanceEntity caseInstance = caseInstanceEntityManager.findById(delegatePlanItemInstance.getReferenceId());
            if (caseInstance != null && !caseInstance.isDeleted()) {
                caseInstanceEntityManager.delete(caseInstance.getId(), cascade, null);
            }
            
        } else {
            throw new FlowableException("Can only delete a child entity for a plan item with callback type " + CallbackTypes.PLAN_ITEM_CHILD_CASE);
        }
    }

    protected void handleOutParameters(DelegatePlanItemInstance planItemInstance,
                                       CaseInstanceEntity caseInstance,
                                       CmmnRuntimeService cmmnRuntimeService, CmmnEngineConfiguration cmmnEngineConfiguration) {
        if (outParameters == null) {
            return;
        }

        for (IOParameter outParameter : outParameters) {

            String variableName = null;
            if (StringUtils.isNotEmpty(outParameter.getTarget())) {
                variableName = outParameter.getTarget();

            } else if (StringUtils.isNotEmpty(outParameter.getTargetExpression())) {
                Object variableNameValue = resolveExpression(cmmnEngineConfiguration, planItemInstance.getReferenceId(), outParameter.getTargetExpression());
                if (variableNameValue != null) {
                    variableName = variableNameValue.toString();
                } else {
                    LOGGER.warn("Out parameter target expression {} did not resolve to a variable name, this is most likely a programmatic error",
                            outParameter.getTargetExpression());
                }

            }

            Object variableValue = null;
            if (StringUtils.isNotEmpty(outParameter.getSourceExpression())) {
                variableValue = resolveExpression(cmmnEngineConfiguration, planItemInstance.getReferenceId(), outParameter.getSourceExpression());

            } else if (StringUtils.isNotEmpty(outParameter.getSource())) {
                variableValue = cmmnRuntimeService.getVariable(planItemInstance.getReferenceId(), outParameter.getSource());

            }
            caseInstance.setVariable(variableName, variableValue);
        }
    }


}
