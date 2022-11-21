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
package org.flowable.cmmn.engine.impl.criteria;

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.model.PlanItem;

/**
 * Internal class used during criteria evaluation.
 *
 * @author Joram Barrez
 */
public class PlanItemLifeCycleEvent {

    protected PlanItemInstanceEntity planItemInstanceEntity;
    protected String transition;
    
    public PlanItemLifeCycleEvent(PlanItemInstanceEntity planItemInstanceEntity, String transition) {
        this.planItemInstanceEntity = planItemInstanceEntity;
        this.transition = transition;
    }

    public PlanItemInstanceEntity getPlanItemInstanceEntity() {
        return planItemInstanceEntity;
    }

    public PlanItem getPlanItem() {
        return planItemInstanceEntity.getPlanItem();
    }

    public String getTransition() {
        return transition;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PlanItem lifecycle event for plan item instance [")
                     .append(planItemInstanceEntity.getId())
                     .append("] with plan item [")
                     .append(getPlanItem())
                     .append("]");
        if (transition != null) {
            stringBuilder.append(" and transition '").append(transition).append("'");
        }
        return stringBuilder.toString();
    }

}
