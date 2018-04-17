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

import org.flowable.cmmn.api.PlanItemInstanceCallbackType;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceBuilderImpl;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class CaseTaskActivityBehavior extends TaskActivityBehavior implements PlanItemActivityBehavior {

    protected Expression caseRefExpression;

    public CaseTaskActivityBehavior(Expression caseRefExpression,CaseTask caseTask) {
        super(caseTask.isBlocking(), caseTask.getBlockingExpression());
        this.caseRefExpression = caseRefExpression;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {

        CaseInstanceHelper caseInstanceHelper = CommandContextUtil.getCaseInstanceHelper(commandContext);
        CaseInstanceBuilder caseInstanceBuilder = new CaseInstanceBuilderImpl().caseDefinitionKey(caseRefExpression.getValue(planItemInstanceEntity).toString());
        CaseInstanceEntity caseInstanceEntity = caseInstanceHelper.startCaseInstance(caseInstanceBuilder);
        caseInstanceEntity.setParentId(planItemInstanceEntity.getCaseInstanceId());

        // Bidirectional storing of reference to avoid queries later on
        caseInstanceEntity.setCallbackType(PlanItemInstanceCallbackType.CHILD_CASE);
        caseInstanceEntity.setCallbackId(planItemInstanceEntity.getId());

        planItemInstanceEntity.setReferenceType(PlanItemInstanceCallbackType.CHILD_CASE);
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
        if (!PlanItemInstanceCallbackType.CHILD_CASE.equals(planItemInstance.getReferenceType())) {
            throw new FlowableException("Cannot trigger case task plan item instance : reference type '"
                    + planItemInstance.getReferenceType() + "' not supported");
        }

        // Triggering the plan item (as opposed to a regular complete) terminates the case instance
        CommandContextUtil.getAgenda(commandContext).planTerminateCaseInstanceOperation(planItemInstance.getReferenceId(), true);
        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstance);
    }

    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {
        if (PlanItemTransition.TERMINATE.equals(transition) || PlanItemTransition.EXIT.equals(transition)) {
            // The plan item will be deleted by the regular TerminatePlanItemOperation
            CommandContextUtil.getAgenda(commandContext).planTerminateCaseInstanceOperation(planItemInstance.getReferenceId(), true);
        }
    }

}
