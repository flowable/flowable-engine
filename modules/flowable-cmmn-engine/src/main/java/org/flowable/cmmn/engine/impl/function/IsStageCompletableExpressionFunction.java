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

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;

/**
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
                return planItemInstanceEntity.isCompleteable();

            } else if (planItemInstanceEntity.getStageInstanceId() != null) {
                PlanItemInstanceEntity stagePlanItemInstanceEntity = planItemInstanceEntity.getStagePlanItemInstanceEntity();
                return stagePlanItemInstanceEntity.isCompleteable();

            } else {
                CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager().findById(planItemInstanceEntity.getCaseInstanceId());
                return caseInstanceEntity.isCompleteable();

            }

        } else if (object instanceof CaseInstanceEntity) {
            CaseInstanceEntity caseInstanceEntity = (CaseInstanceEntity) object;
            return caseInstanceEntity.isCompleteable();

        }
        return false;
    }

}
