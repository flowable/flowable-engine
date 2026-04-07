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
package org.flowable.cmmn.engine.impl.util;

import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.api.delegate.BusinessError;
import org.flowable.common.engine.impl.callback.CallbackData;
import org.flowable.common.engine.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;

/**
 * Handles propagation of a {@link BusinessError} during CMMN plan item execution.
 *
 * <ol>
 *   <li>Check if any plan item in the case has a fault sentry on the faulting task (in a catchable state)</li>
 *   <li>If found: fail the task, sentries fire naturally via the standard sentry evaluation. Done.</li>
 *   <li>If not found: propagate to parent engine via callback (async) or re-throw (synchronous/no parent)</li>
 * </ol>
 *
 * This respects CMMN semantics — sentries can reference plan items across stages,
 * so there is no need to walk the stage hierarchy level by level.
 *
 * @author Joram Barrez
 */
public class FaultPropagation {

    public static void propagateFault(BusinessError fault, CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        // The BusinessError is carried on the PlanItemLifeCycleEvent and resolved via
        // CmmnFaultVariableContainer during sentry if-part evaluation — similar to BPMN's BpmnErrorVariableContainer.

        // Check if any plan item in the case has a fault sentry on this plan item.
        // Uses the parser-built dependency graph (entryDependentPlanItems) to avoid
        // walking the entire plan item instance tree at runtime.
        if (isFaultCaught(planItemInstanceEntity)) {
            // Fault is caught — fail the task (passing the BusinessError for sentry evaluation)
            // and let the standard sentry evaluation fire
            CommandContextUtil.getAgenda(commandContext).planFailPlanItemInstanceOperation(planItemInstanceEntity, fault);
        } else {
            // No sentry catches this fault.
            // If the case has a parent engine (started via CaseTask/ProcessTask), propagate the error.
            CaseInstanceEntity caseInstance = CommandContextUtil.getCaseInstanceEntityManager()
                    .findById(planItemInstanceEntity.getCaseInstanceId());

            if (caseInstance != null && caseInstance.getCallbackId() != null && caseInstance.getCallbackType() != null
                    && !commandContext.isReused()) {
                // Async case (separate transaction) — route via onError callback
                CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
                Map<String, List<RuntimeInstanceStateChangeCallback>> callbacks =
                        cmmnEngineConfiguration.getCaseInstanceStateChangeCallbacks();
                if (callbacks != null && callbacks.containsKey(caseInstance.getCallbackType())) {
                    CallbackData callbackData = new CallbackData(caseInstance.getCallbackId(),
                            caseInstance.getCallbackType(), caseInstance.getId(), null, null);
                    for (RuntimeInstanceStateChangeCallback callback : callbacks.get(caseInstance.getCallbackType())) {
                        callback.onError(callbackData, fault);
                    }
                    // Fail the plan item (the error was handled by the parent engine)
                    CommandContextUtil.getAgenda(commandContext).planFailPlanItemInstanceOperation(planItemInstanceEntity, fault);
                    return;
                }
            }

            // Re-throw — synchronous case (parent catches via try/catch), no parent, or no callback configured.
            throw fault;
        }
    }

    /**
     * Checks if any plan item in the case has a fault sentry referencing the faulting plan item,
     * using the parser-built dependency graph ({@link PlanItem#getEntryDependentPlanItems()}).
     * Also verifies that at least one runtime instance of the dependent plan item is in a
     * catchable state (AVAILABLE or WAITING_FOR_REPETITION).
     */
    public static boolean isFaultCaught(PlanItemInstanceEntity faultedPlanItemInstance) {
        PlanItem faultedPlanItem = faultedPlanItemInstance.getPlanItem();
        if (faultedPlanItem == null) {
            return false;
        }

        // The parser already computed which plan items depend on this one via entry/exit criteria
        List<PlanItem> allDependentPlanItems = faultedPlanItem.getAllDependentPlanItems();
        if (allDependentPlanItems == null || allDependentPlanItems.isEmpty()) {
            return false;
        }

        String faultedPlanItemId = faultedPlanItem.getId();
        String caseInstanceId = faultedPlanItemInstance.getCaseInstanceId();

        for (PlanItem dependentPlanItem : allDependentPlanItems) {
            // Check entry criteria — dependent must be in AVAILABLE or WAITING_FOR_REPETITION
            if (hasFaultSentryOnSource(dependentPlanItem.getEntryCriteria(), faultedPlanItemId)) {
                if (hasPlanItemInstanceInState(caseInstanceId, dependentPlanItem.getId(), PlanItemInstanceState.EVALUATE_ENTRY_CRITERIA_STATES)) {
                    return true;
                }
            }
            // Check exit criteria — dependent must be in ACTIVE state (can only exit active items)
            if (hasFaultSentryOnSource(dependentPlanItem.getExitCriteria(), faultedPlanItemId)) {
                if (hasPlanItemInstanceInState(caseInstanceId, dependentPlanItem.getId(), PlanItemInstanceState.ACTIVE_STATES)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if any of the given criteria has a sentry that references
     * the source plan item with standardEvent="fault".
     */
    private static boolean hasFaultSentryOnSource(List<Criterion> criteria, String sourcePlanItemId) {
        if (criteria == null) {
            return false;
        }
        for (Criterion criterion : criteria) {
            Sentry sentry = criterion.getSentry();
            if (sentry != null) {
                for (SentryOnPart onPart : sentry.getOnParts()) {
                    if (onPart.getSource() != null
                            && onPart.getSource().getId().equals(sourcePlanItemId)
                            && PlanItemTransition.FAULT.equals(onPart.getStandardEvent())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if any runtime plan item instance for the given plan item definition ID
     * is in one of the given states.
     */
    private static boolean hasPlanItemInstanceInState(String caseInstanceId, String planItemId, java.util.Set<String> states) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        List<PlanItemInstanceEntity> instances = cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                .findByCaseInstanceIdAndPlanItemId(caseInstanceId, planItemId);
        if (instances != null) {
            for (PlanItemInstanceEntity instance : instances) {
                if (states.contains(instance.getState())) {
                    return true;
                }
            }
        }
        return false;
    }
}
