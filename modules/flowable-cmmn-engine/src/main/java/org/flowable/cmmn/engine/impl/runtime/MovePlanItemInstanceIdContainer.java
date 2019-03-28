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
public class MovePlanItemInstanceIdContainer {

    protected List<String> planItemInstanceIds;
    protected List<String> moveToPlanItemDefinitionIds;
    protected String newAssigneeId;

    public MovePlanItemInstanceIdContainer(String singlePlanItemInstanceId, String moveToPlanItemDefinitionId) {
        this(singlePlanItemInstanceId, moveToPlanItemDefinitionId, null);
    }

    public MovePlanItemInstanceIdContainer(String singlePlanItemInstanceId, String moveToPlanItemDefinitionId, String newAssigneeId) {
        this.planItemInstanceIds = Collections.singletonList(singlePlanItemInstanceId);
        this.moveToPlanItemDefinitionIds = Collections.singletonList(moveToPlanItemDefinitionId);
        this.newAssigneeId = newAssigneeId;
    }

    public MovePlanItemInstanceIdContainer(List<String> planItemInstanceIds, String moveToPlanItemDefinitionId) {
        this(planItemInstanceIds, moveToPlanItemDefinitionId, null);
    }

    public MovePlanItemInstanceIdContainer(List<String> planItemInstanceIds, String moveToPlanItemDefinitionId, String newAssigneeId) {
        this.planItemInstanceIds = planItemInstanceIds;
        this.moveToPlanItemDefinitionIds = Collections.singletonList(moveToPlanItemDefinitionId);
        this.newAssigneeId = newAssigneeId;
    }

    public MovePlanItemInstanceIdContainer(String singlePlanItemInstanceId, List<String> moveToPlanItemDefinitionIds) {
        this(singlePlanItemInstanceId, moveToPlanItemDefinitionIds, null);
    }

    public MovePlanItemInstanceIdContainer(String singlePlanItemInstanceId, List<String> moveToPlanItemDefinitionIds, String newAssigneeId) {
        this.planItemInstanceIds = Collections.singletonList(singlePlanItemInstanceId);
        this.moveToPlanItemDefinitionIds = moveToPlanItemDefinitionIds;
        this.newAssigneeId = newAssigneeId;
    }

    public List<String> getPlanItemInstanceIds() {
        return planItemInstanceIds;
    }

    public void setPlanItemInstanceIds(List<String> planItemInstanceIds) {
        this.planItemInstanceIds = planItemInstanceIds;
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
