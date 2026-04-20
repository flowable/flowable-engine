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
package org.flowable.cmmn.engine.impl.agenda.operation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.impl.agenda.PlanItemEvaluationResult;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceContainer;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.ExpressionUtil;
import org.flowable.cmmn.engine.impl.util.PlanItemInstanceUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.PlanFragment;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemControl;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ReactivationRule;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * An abstract base class for CMMN engine based operations supporting general functionalities.
 *
 * @author Joram Barrez
 * @author Micha Kiener
 */
public abstract class CmmnOperation implements Runnable {
    
    protected CommandContext commandContext;
    protected boolean isNoop = false; // flag indicating whether this operation did something. False by default, as all operation should typically do something.
    
    public CmmnOperation() {
    }

    public CmmnOperation(CommandContext commandContext) {
        this.commandContext = commandContext;
    }

    /**
     * Called when the operation is planned on the agenda (but not yet executed)
     */
    public void onPlanned() {
        // No-op by default
    }

    /**
     * @return The id of the case instance related to this operation.
     */
    public abstract String getCaseInstanceId();

    /**
     * Returns the case instance entity using the entity manager by getting the case instance id through {@link #getCaseInstanceId()} and returning
     * the case instance entity accordingly. If there is no id provided, this method will return null, but not throw an exception.
     *
     * @return the case instance entity according the case instance id involved in this operation or null, if there is none involved
     */
    protected CaseInstanceEntity getCaseInstance() {
        String caseId = getCaseInstanceId();
        if (caseId == null) {
            return null;
        }

        return CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseId);
    }

    protected Stage getStage(PlanItemInstanceEntity planItemInstanceEntity) {
        PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
        if (planItemDefinition instanceof Stage) {
            return (Stage) planItemDefinition;
        } else {
            return planItemDefinition.getParentStage();
        }
    }

    public boolean isStage(PlanItemInstanceEntity planItemInstanceEntity) {
        return (planItemInstanceEntity.getPlanItem() != null
                && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() != null
                && planItemInstanceEntity.getPlanItem().getPlanItemDefinition() instanceof Stage);
    }

    public Stage getPlanModel(CaseInstanceEntity caseInstanceEntity) {
        return CaseDefinitionUtil.getCase(caseInstanceEntity.getCaseDefinitionId()).getPlanModel();
    }
    
    
    /**
     * Creates child plan items for the provided stage, might also be the root plan model. It supports both scenarios: a case, running the first time or even
     * a reactivated one which has some reactivation rules to be considered when creating child plan items.
     *
     * @param commandContext the command context to run this method with
     * @param caseModel the case model the given stage or plan model is contained within
     * @param planItems the plan items of the stage to be checked for creation
     * @param directlyReactivatedPlanItems an optional list of plan items already having been reactivated as part of phase 1 of a case reactivation, might be
     *      null or empty, specially for a regular case
     * @param caseInstanceEntity the case instance entity, must be provided as it is used to check for a first time running case or a reactivated one
     * @param stagePlanItemInstanceEntity the optional stage plan item instance, if already available, might be null in very specific use cases (e.g. cross-stage activities, etc)
     * @param tenantId the id of the tenant to run within
     * @return the list of created plan item instances for this stage or plan model
     */
    protected List<PlanItemInstanceEntity> createPlanItemInstancesForNewOrReactivatedStage(CommandContext commandContext, Case caseModel, List<PlanItem> planItems,
            List<PlanItem> directlyReactivatedPlanItems, CaseInstanceEntity caseInstanceEntity, PlanItemInstanceEntity stagePlanItemInstanceEntity, String tenantId) {
        
        List<PlanItemInstanceEntity> newPlanItemInstances = new ArrayList<>();
        for (PlanItem planItem : planItems) {

            if (planItem.isInstanceLifecycleEnabled()) {
                // check, if the plan item was already reactivated as part of phase 1 of a case reactivation and if so, skip it to prevent it from being
                // reactivated more than once
                if (directlyReactivatedPlanItems == null || directlyReactivatedPlanItems.stream().noneMatch(i -> i.getId().equals(planItem.getId()))) {
                    createPlanItemInstanceIfNeeded(commandContext, planItem, caseModel, caseInstanceEntity,
                        stagePlanItemInstanceEntity, tenantId, newPlanItemInstances);
                }

            } else if (planItem.getPlanItemDefinition() != null && planItem.getPlanItemDefinition() instanceof PlanFragment planFragment) {
                // Some plan items (plan fragments) exist as plan item, but not as plan item instance
                List<PlanItem> planFragmentPlanItems = planFragment.getDirectChildPlanItemsWithLifecycleEnabled();
                for (PlanItem planFragmentPlanItem : planFragmentPlanItems) {
                    createPlanItemInstanceIfNeeded(commandContext, planFragmentPlanItem, caseModel, caseInstanceEntity,
                        stagePlanItemInstanceEntity, tenantId, newPlanItemInstances);
                }

            }
        }
        return newPlanItemInstances;
    }

    /**
     * Checks whether the provided plan item needs an instance to be created and optionally activated directly (e.g. for a reactivation use case). It checks
     * the plan items reactivation model, if the case is a reactivated one.
     *
     * @param commandContext the command context in which this method runs
     * @param planItem the plan item to check for an item creation
     * @param caseModel the case model used to get the creation mode
     * @param caseInstanceEntity the case entity used to check, if it is a reactivated one
     * @param stagePlanItemInstanceEntity the stage the plan item belongs to
     * @param tenantId the id of the tenant the case belongs to
     * @param newPlanItemInstances the array where the new plan item instance will be added, if it was created
     */
    protected void createPlanItemInstanceIfNeeded(CommandContext commandContext, PlanItem planItem, Case caseModel, CaseInstanceEntity caseInstanceEntity,
            PlanItemInstanceEntity stagePlanItemInstanceEntity, String tenantId, List<PlanItemInstanceEntity> newPlanItemInstances) {

        // In some cases (e.g. cross-border triggering of a sentry, the child plan item instance has been activated already
        // As such, it doesn't need to be created again (this is the if check here, which goes against the cache)
        if (stagePlanItemInstanceEntity == null || !childPlanItemInstanceForPlanItemExists(stagePlanItemInstanceEntity, planItem)) {

            // check if we are in a reactivated case as creating new plan items is depending on the reactivation rule of the plan item
            // or the default one of the reactivation event listener
            PlanItemCreationType creationType = getPlanItemCreationOrReactivationType(caseInstanceEntity, caseModel, planItem, stagePlanItemInstanceEntity);

            if (!creationType.isTypeIgnore()) {

                String caseInstanceId = null;
                if (caseInstanceEntity != null) {
                    caseInstanceId = caseInstanceEntity.getId();
                } else if (stagePlanItemInstanceEntity != null) {
                    caseInstanceId = stagePlanItemInstanceEntity.getCaseInstanceId();
                }

                PlanItemInstanceEntity childPlanItemInstance = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext)
                    .createPlanItemInstanceEntityBuilder()
                    .planItem(planItem)
                    .caseDefinitionId(caseInstanceEntity.getCaseDefinitionId())
                    .caseInstanceId(caseInstanceId)
                    .stagePlanItemInstance(stagePlanItemInstanceEntity)
                    .tenantId(tenantId)
                    .addToParent(true)
                    // we silently ignore any exceptions evaluating the name for the new plan item, if it has repetition on a collection, as the item / itemIndex
                    // local variables might not yet be available
                    .silentNameExpressionEvaluation(ExpressionUtil.hasRepetitionOnCollection(planItem))
                    .create();
                
                PlanItemEvaluationResult evaluationResult = null;
                if (creationType.isTypeActivate()) {
                    evaluationResult = new PlanItemEvaluationResult();
                    PlanItemInstanceUtil.evaluateRepetitionRule(childPlanItemInstance, null, stagePlanItemInstanceEntity,
                            evaluationResult, commandContext);
                }
                
                newPlanItemInstances.add(childPlanItemInstance);
                
                if (creationType.isTypeActivate() && evaluationResult.getNewChildPlanItemInstances() != null && !evaluationResult.getNewChildPlanItemInstances().isEmpty()) {
                    newPlanItemInstances.addAll(evaluationResult.getNewChildPlanItemInstances());
                }

                // for default or activate creation type, we need to plan for the create operation
                CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceOperation(childPlanItemInstance);

                // additionally, we plan for the activation operation, if we directly need to activate the plan item without any further evaluation
                if (creationType.isTypeActivate()) {
                    CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstanceOperation(childPlanItemInstance, null);
                }
            }
        }
    }

    protected boolean childPlanItemInstanceForPlanItemExists(PlanItemInstanceContainer planItemInstanceContainer, PlanItem planItem) {
        List<PlanItemInstanceEntity> childPlanItemInstances = planItemInstanceContainer.getChildPlanItemInstances();
        if (childPlanItemInstances != null && !childPlanItemInstances.isEmpty()) {
            for (PlanItemInstanceEntity childPlanItemInstanceEntity : childPlanItemInstances) {
                if (childPlanItemInstanceEntity.getPlanItem() != null && planItem.getId().equals(childPlanItemInstanceEntity.getPlanItem().getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isEventListenerWithAvailableCondition(PlanItem planItem) {
        if (planItem != null && planItem.getPlanItemDefinition() != null && planItem.getPlanItemDefinition() instanceof EventListener eventListener) {
            return StringUtils.isNotEmpty(eventListener.getAvailableConditionExpression());
        }
        return false;
    }
    
    protected void setRepetitionCounter(PlanItemInstanceEntity repeatingPlanItemInstanceEntity, int counterValue) {
        repeatingPlanItemInstanceEntity.setVariableLocal(PlanItemInstanceUtil.getCounterVariable(repeatingPlanItemInstanceEntity), counterValue);
    }

    /**
     * Evaluates the reactivation rule for the provided plan item, if the case is a reactivated one, otherwise the creation type will always be default.
     * For a reactivated case, the plan items optional reactivation rule is considered and its creation type returned. If there is none but there is a default
     * one specified on the reactivation listener, that creation type is returned instead.
     *
     * @param caseInstanceEntity the case instance to check for a reactivated case
     * @param caseModel the case model to obtain the reactivation listener
     * @param planItem the plan item to be evaluated for creation type
     * @param parentPlanItemInstance the parent plan item (e.g. stage or plan model) of the plan item
     * @return the plan item creation type according all the rules for creation and reactivation
     */
    protected PlanItemCreationType getPlanItemCreationOrReactivationType(CaseInstanceEntity caseInstanceEntity, Case caseModel, 
            PlanItem planItem, VariableContainer parentPlanItemInstance) {

        // if the case was never reactivated, we can directly return as there is no special rules to be considered during creation of plan items
        if (caseInstanceEntity.getLastReactivationTime() == null) {
            return PlanItemCreationType.typeDefault();
        }

        // if the case was reactivated, we need to check the reactivation rules, if any specified on the plan item or a default one on the listener
        PlanItemControl itemControl = planItem.getItemControl();
        if (itemControl != null && itemControl.getReactivationRule() != null) {
            // evaluate the specific reactivation rule on the plan item directly as a first step
            PlanItemCreationType creationType = evaluateReactivationRule(itemControl.getReactivationRule(), caseInstanceEntity, parentPlanItemInstance);
            if (creationType != null) {
                return creationType;
            }
        }

        // if there is no reactivation rule provided explicitly, or it didn't match any of its conditions, we need to check the reactivation listener as it
        // might contain the default reactivation rule for plan items
        if (caseModel.getReactivateEventListener() != null && caseModel.getReactivateEventListener().getDefaultReactivationRule() != null) {
            PlanItemCreationType creationType = evaluateReactivationRule(caseModel.getReactivateEventListener().getDefaultReactivationRule(),
                caseInstanceEntity, parentPlanItemInstance);

            // return the default creation type, if the rule matched
            if (creationType != null) {
                return creationType;
            }
        }

        // return the default type which is the same like with regular case creation, if no matching rules found explicitly on the plan item or event listener
        return PlanItemCreationType.typeDefault();
    }

    /**
     * Evaluates the provided reactivation rule for a matching rule and returns its creation type, if at least one was matching. The rules are evaluated the
     * following way:
     * <ul>
     *     <li>reactivation condition: only if explicitly evaluating to true, that type is returned, if false or not existing, it is not considered</li>
     *     <li>ignore condition: only if explicitly evaluating to true, that type is returned, if false or not existing, it is not considered</li>
     *     <li>default condition: if explicitly evaluating to either true OR false, type default or type ignore is returned, only if there is no condition, nothing is returned</li>
     * </ul>
     * Or in other words: if there is an explicit matching activate or ignore rule, activate or ignore is returned, if there is an explicit default rule,
     * default is returned if it evaluates to true, ignore if it evaluates to false or null, if none of the above is met.
     *
     * @param reactivationRule the reactivation rule to evaluate against the given context of either the parent plan item instance or the case instance itself
     * @param caseInstanceEntity the case instance which must not be null
     * @param parentPlanItemInstance the optional parent plan item instance (e.g. stage or plan model), if already existing, might be null
     * @return the evaluated creation type, if explicitly matched against one of the rules, null, if no matching rule was found
     */
    protected PlanItemCreationType evaluateReactivationRule(ReactivationRule reactivationRule, CaseInstanceEntity caseInstanceEntity, VariableContainer parentPlanItemInstance) {
        // first evaluate for an activate condition
        Boolean condition = evaluateReactivationCondition(reactivationRule.getActivateCondition(), caseInstanceEntity, parentPlanItemInstance);
        if (condition != null && condition) {
            // only return for an activation, if explicitly evaluated to true
            return PlanItemCreationType.typeActivate();
        }

        // next evaluate for an ignore condition
        condition = evaluateReactivationCondition(reactivationRule.getIgnoreCondition(), caseInstanceEntity, parentPlanItemInstance);
        if (condition != null && condition) {
            // only return ignore, if explicitly evaluated to true
            return PlanItemCreationType.typeIgnore();
        }

        // next evaluate for a default condition
        condition = evaluateReactivationCondition(reactivationRule.getDefaultCondition(), caseInstanceEntity, parentPlanItemInstance);
        if (condition != null) {
            // return on an explicit result, even if false
            if (condition) {
                return PlanItemCreationType.typeDefault();
            }
            return PlanItemCreationType.typeIgnore();
        }
        return null;
    }

    /**
     * Evaluates the provided reactivation condition, which might also be null or a constant like "true". If it is a condition, it is evaluated in
     * the context of the provided variable container.
     *
     * @param condition the condition to be evaluated, might be null or a constant like "true"
     * @param caseInstanceEntity the case instance to be used as the expression context, if the parent plan item is not yet available (null)
     * @param parentPlanItemInstance the optional parent plan item to be used as the expression evaluation context
     * @return the condition evaluation result
     */
    protected Boolean evaluateReactivationCondition(String condition, CaseInstanceEntity caseInstanceEntity, VariableContainer parentPlanItemInstance) {
        if (StringUtils.isEmpty(condition)) {
            return null;
        }
        if (Boolean.parseBoolean(condition)) {
            return true;
        }
        return ExpressionUtil.evaluateBooleanExpression(commandContext, parentPlanItemInstance == null ? caseInstanceEntity : parentPlanItemInstance, condition);
    }

    public void markAsNoop() {
        isNoop = true;
    }

    public boolean isNoop() {
        return isNoop;
    }

}
