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
package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.cmmn.model.PlanItem;

/**
 * An interface for entities that have child {@link org.flowable.cmmn.api.runtime.PlanItemInstance}s.
 *
 * @author Joram Barrez
 */
public interface PlanItemInstanceContainer {

    /**
     * @return All the {@link PlanItem}'s definition in the {@link org.flowable.cmmn.model.CmmnModel}.
     */
    List<PlanItem> getPlanItems();

    /**
     * @return All child plan item instances that are not in a terminal state.
     */
    List<PlanItemInstanceEntity> getChildPlanItemInstances();

    /**
     * @param childPlanItemInstances Sets the child plan item instances of this container.
     */
    void setChildPlanItemInstances(List<PlanItemInstanceEntity> childPlanItemInstances);
    
}
