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
package org.flowable.cmmn.engine.impl.function;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.PlanItemInstanceContainerUtil;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.PlanItemDefinition;

/**
 * This function evaluates a stage to be completable, which is the case, if all required and active plan items are completed
 * @author Joram Barrez
 */
public class IsStageCompletableExpressionFunction extends AbstractCmmnExpressionFunction {

    public IsStageCompletableExpressionFunction() {
        super("isStageCompletable");
    }

    @Override
    protected boolean isMultiParameterFunction() {
        return false;
    }

    @Override
    protected boolean isNoParameterMethod() {
        return true;
    }

    public static boolean isStageCompletable(Object object) {
        if (object instanceof PlanItemInstanceEntity) {
            PlanItemInstanceEntity planItemInstanceEntity = (PlanItemInstanceEntity) object;

            if (planItemInstanceEntity.isStage()) {
                return planItemInstanceEntity.isCompletable();

            } else if (planItemInstanceEntity.getStageInstanceId() != null) {
                PlanItemInstanceEntity stagePlanItemInstanceEntity = planItemInstanceEntity.getStagePlanItemInstanceEntity();

                // Special care needed for the event listeners with an available condition: a new evaluation needs to be done
                // as the completable only gets set at the end of the evaluation cycle.

                PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
                if (PlanItemInstanceState.AVAILABLE.equals(planItemInstanceEntity.getState())
                        && planItemDefinition instanceof EventListener
                        && (StringUtils.isNotEmpty(((EventListener) planItemDefinition).getAvailableConditionExpression()))) {
                    return PlanItemInstanceContainerUtil.shouldPlanItemContainerComplete(stagePlanItemInstanceEntity,
                        Collections.singletonList(planItemInstanceEntity.getId()), true).isCompletable();

                } else {
                    return stagePlanItemInstanceEntity.isCompletable();

                }

            } else {
                CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager().findById(planItemInstanceEntity.getCaseInstanceId());
                return caseInstanceEntity.isCompletable();

            }

        } else if (object instanceof CaseInstanceEntity) {
            CaseInstanceEntity caseInstanceEntity = (CaseInstanceEntity) object;
            return caseInstanceEntity.isCompletable();

        }
        return false;
    }

}
