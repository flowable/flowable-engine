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
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceBuilderImpl;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.constant.ReferenceTypes;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.api.FormService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class CaseTaskActivityBehavior extends ChildTaskActivityBehavior implements PlanItemActivityBehavior {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseTaskActivityBehavior.class);

    protected Expression caseRefExpression;
    protected String caseRef;
    protected Boolean fallbackToDefaultTenant;
    protected boolean sameDeployment;
    protected CaseTask caseTask;

    public CaseTaskActivityBehavior(Expression caseRefExpression, CaseTask caseTask) {
        super(caseTask.isBlocking(), caseTask.getBlockingExpression(), caseTask.getInParameters(), caseTask.getOutParameters());
        this.caseRefExpression = caseRefExpression;
        this.caseRef = caseTask.getCaseRef();
        this.fallbackToDefaultTenant = caseTask.getFallbackToDefaultTenant();
        this.sameDeployment = caseTask.isSameDeployment();
        this.caseTask = caseTask;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, ChildTaskActivityBehavior.VariableInfo variableInfo) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        CaseInstanceHelper caseInstanceHelper = CommandContextUtil.getCaseInstanceHelper(commandContext);

        String caseDefinitionKey = null;
        if (caseRefExpression != null) {
            caseDefinitionKey = caseRefExpression.getValue(planItemInstanceEntity).toString();

        } else if (StringUtils.isNotEmpty(caseRef)) {
            caseDefinitionKey = caseRef;

        }
        if (StringUtils.isEmpty(caseDefinitionKey)) {
            throw new FlowableException("Could not start case instance: no case reference defined");
        }

        CaseInstanceBuilder caseInstanceBuilder = new CaseInstanceBuilderImpl().caseDefinitionKey(caseDefinitionKey);
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

        // Needed for the form field handler later
        Map<String, Object> variablesFromFormSubmission = null;

        if (variableInfo != null) {

            if (variableInfo.formInfo != null) {
                FormService formService = CommandContextUtil.getFormService(commandContext);
                if (formService == null) {
                    throw new FlowableIllegalStateException("Form engine is not initialized");
                }

                variablesFromFormSubmission = formService
                        .getVariablesFromFormSubmission(variableInfo.formInfo, variableInfo.formVariables, variableInfo.formOutcome);

                finalVariableMap.putAll(variablesFromFormSubmission);
            }

            if (variableInfo.variables != null && !variableInfo.variables.isEmpty()) {
                finalVariableMap.putAll(variableInfo.variables);
            }
        }

        caseInstanceBuilder.businessKey(getBusinessKey(cmmnEngineConfiguration, planItemInstanceEntity, caseTask));
        caseInstanceBuilder.variables(finalVariableMap);

        if (sameDeployment) {
            caseInstanceBuilder.caseDefinitionParentDeploymentId(
                    CaseDefinitionUtil.getDefinitionDeploymentId(planItemInstanceEntity.getCaseDefinitionId(), cmmnEngineConfiguration));
        }

        boolean blocking = evaluateIsBlocking(planItemInstanceEntity);
        if (blocking) {
            caseInstanceBuilder.callbackType(CallbackTypes.PLAN_ITEM_CHILD_CASE);
            caseInstanceBuilder.callbackId(planItemInstanceEntity.getId());
        }

        CaseInstanceEntity caseInstanceEntity = caseInstanceHelper.startCaseInstance(caseInstanceBuilder);

        if (StringUtils.isNotEmpty(caseTask.getCaseInstanceIdVariableName())) {
            Expression expression = cmmnEngineConfiguration.getExpressionManager().createExpression(caseTask.getCaseInstanceIdVariableName());
            String idVariableName = (String) expression.getValue(planItemInstanceEntity);
            if (StringUtils.isNotEmpty(idVariableName)) {
                planItemInstanceEntity.setVariable(idVariableName, caseInstanceEntity.getId());
            }
        }

        // Bidirectional storing of reference to avoid queries later on
        planItemInstanceEntity.setReferenceType(ReferenceTypes.PLAN_ITEM_CHILD_CASE);
        planItemInstanceEntity.setReferenceId(caseInstanceEntity.getId());

        if (variablesFromFormSubmission != null && !variablesFromFormSubmission.isEmpty()) {
            // The variablesFromFormSubmission can only be non null if there was formInfo
            cmmnEngineConfiguration.getFormFieldHandler()
                    .handleFormFieldsOnSubmit(variableInfo.formInfo, null, null, caseInstanceEntity.getId(), ScopeTypes.CMMN,
                            variablesFromFormSubmission, caseInstanceEntity.getTenantId());
        }

        if (!blocking) {
            CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstanceEntity);
        }
    }

    @Override
    public void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstance) {
        if (!PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState())) {
            throw new FlowableIllegalStateException("Can only trigger a plan item that is in the ACTIVE state");
        }
        if (planItemInstance.getReferenceId() == null) {
            throw new FlowableIllegalStateException("Cannot trigger case task plan item instance : no reference id set");
        }
        if (!ReferenceTypes.PLAN_ITEM_CHILD_CASE.equals(planItemInstance.getReferenceType())) {
            throw new FlowableIllegalStateException("Cannot trigger case task plan item instance : reference type '"
                    + planItemInstance.getReferenceType() + "' not supported");
        }

        // Need to be set before planning the complete operation
        handleOutParameters(commandContext, planItemInstance);

        // Triggering the plan item (as opposed to a regular complete) terminates the case instance
        CommandContextUtil.getAgenda(commandContext).planManualTerminateCaseInstanceOperation(planItemInstance.getReferenceId());
        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstance);
    }

    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {
        if (PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState())) {

            if (PlanItemTransition.EXIT.equals(transition)) {

                // Typically this happens when terminating through an exit sentry
                // There is out parameter handling, consistent with the case task in BPMN

                handleOutParameters(commandContext, (PlanItemInstanceEntity) planItemInstance);
                CommandContextUtil.getAgenda(commandContext).planManualTerminateCaseInstanceOperation(planItemInstance.getReferenceId());

            }

            if (PlanItemTransition.TERMINATE.equals(transition)) {

                // Typically this happens when terminating a case instance through the API
                // The plan item will be deleted by the regular TerminatePlanItemOperation
                // There is no out parameter handling as the case instance is forced terminated.

                CommandContextUtil.getAgenda(commandContext).planManualTerminateCaseInstanceOperation(planItemInstance.getReferenceId());

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
        if (ReferenceTypes.PLAN_ITEM_CHILD_CASE.equals(delegatePlanItemInstance.getReferenceType())) {
            CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
            CaseInstanceEntity caseInstance = caseInstanceEntityManager.findById(delegatePlanItemInstance.getReferenceId());
            if (caseInstance != null && !caseInstance.isDeleted()) {
                CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
                cmmnEngineConfiguration.getCmmnHistoryManager().recordCaseInstanceEnd(
                        caseInstance, CaseInstanceState.TERMINATED, cmmnEngineConfiguration.getClock().getCurrentTime());
                caseInstanceEntityManager.delete(caseInstance.getId(), cascade, null);
            }
            
        } else {
            throw new FlowableException("Can only delete a child entity for a plan item with reference type " + ReferenceTypes.PLAN_ITEM_CHILD_CASE);
        }
    }

    protected void handleOutParameters(CommandContext commandContext, PlanItemInstanceEntity planItemInstance) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        CaseInstanceEntityManager caseInstanceEntityManager = cmmnEngineConfiguration.getCaseInstanceEntityManager();
        CaseInstanceEntity caseInstance = caseInstanceEntityManager.findById(planItemInstance.getCaseInstanceId());
        handleOutParameters(
            planItemInstance,
            caseInstance,
            cmmnEngineConfiguration.getCmmnRuntimeService(),
            cmmnEngineConfiguration);
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
