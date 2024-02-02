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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.Collections;
import java.util.List;

/**
 * @author Tijs Rademakers
 */
public class MovePlanItemDefinitionIdContainer {

    protected List<String> planItemDefinitionIds;
    protected List<String> moveToPlanItemDefinitionIds;
    protected String newAssigneeId;

    public MovePlanItemDefinitionIdContainer(String singlePlanItemDefinitionId, String moveToPlanItemDefinitionId) {
        this(singlePlanItemDefinitionId, moveToPlanItemDefinitionId, null);
    }

    public MovePlanItemDefinitionIdContainer(String singlePlanItemDefinitionId, String moveToPlanItemDefinitionId, String newAssigneeId) {
        this.planItemDefinitionIds = Collections.singletonList(singlePlanItemDefinitionId);
        this.moveToPlanItemDefinitionIds = Collections.singletonList(moveToPlanItemDefinitionId);
        this.newAssigneeId = newAssigneeId;
    }

    public MovePlanItemDefinitionIdContainer(List<String> planItemDefinitionIds, String moveToPlanItemDefinitionId) {
        this(planItemDefinitionIds, moveToPlanItemDefinitionId, null);
    }

    public MovePlanItemDefinitionIdContainer(List<String> planItemDefinitionIds, String moveToPlanItemDefinitionId, String newAssigneeId) {
        this.planItemDefinitionIds = planItemDefinitionIds;
        this.moveToPlanItemDefinitionIds = Collections.singletonList(moveToPlanItemDefinitionId);
        this.newAssigneeId = newAssigneeId;
    }

    public MovePlanItemDefinitionIdContainer(String singlePlanItemDefinitionId, List<String> moveToPlanItemDefinitionIds) {
        this(singlePlanItemDefinitionId, moveToPlanItemDefinitionIds, null);
    }

    public MovePlanItemDefinitionIdContainer(String singlePlanItemDefinitionId, List<String> moveToPlanItemDefinitionIds, String newAssigneeId) {
        this.planItemDefinitionIds = Collections.singletonList(singlePlanItemDefinitionId);
        this.moveToPlanItemDefinitionIds = moveToPlanItemDefinitionIds;
        this.newAssigneeId = newAssigneeId;
    }

    public List<String> getPlanItemDefinitionIds() {
        return planItemDefinitionIds;
    }

    public void setPlanItemDefinitionIds(List<String> planItemDefinitionIds) {
        this.planItemDefinitionIds = planItemDefinitionIds;
    }

    public List<String> getMoveToPlanItemDefinitionIds() {
        return moveToPlanItemDefinitionIds;
    }

    public void setMoveToPlanItemDefinitionIds(List<String> moveToPlanItemDefinitionIds) {
        this.moveToPlanItemDefinitionIds = moveToPlanItemDefinitionIds;
    }

    public String getNewAssigneeId() {
        return newAssigneeId;
    }

    public void setNewAssigneeId(String newAssigneeId) {
        this.newAssigneeId = newAssigneeId;
    }
}
