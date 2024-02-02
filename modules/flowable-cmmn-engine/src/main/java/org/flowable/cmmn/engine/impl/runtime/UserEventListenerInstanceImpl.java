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

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;

/**
 * @author Dennis Federico
 */
public class UserEventListenerInstanceImpl implements UserEventListenerInstance {

    protected PlanItemInstance innerPlanItemInstance;

    public UserEventListenerInstanceImpl(PlanItemInstance planItemInstance) {
        this.innerPlanItemInstance = planItemInstance;
    }

    public static UserEventListenerInstance fromPlanItemInstance(PlanItemInstance planItemInstance) {
        if (planItemInstance == null) {
            return null;
        }
        return new UserEventListenerInstanceImpl(planItemInstance);
    }

    @Override
    public String getId() {
        return innerPlanItemInstance.getId();
    }

    @Override
    public String getName() {
        return innerPlanItemInstance.getName();
    }

    @Override
    public String getCaseInstanceId() {
        return innerPlanItemInstance.getCaseInstanceId();
    }

    @Override
    public String getCaseDefinitionId() {
        return innerPlanItemInstance.getCaseDefinitionId();
    }

    @Override
    public String getElementId() {
        return innerPlanItemInstance.getElementId();
    }

    @Override
    public String getPlanItemDefinitionId() {
        return innerPlanItemInstance.getPlanItemDefinitionId();
    }

    @Override
    public String getStageInstanceId() {
        return innerPlanItemInstance.getStageInstanceId();
    }

    @Override
    public String getState() {
        return innerPlanItemInstance.getState();
    }

    @Override
    public String toString() {
        return "UserEventListenerInstanceImpl{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", caseInstanceId='" + getCaseInstanceId() + '\'' +
                ", caseDefinitionId='" + getCaseDefinitionId() + '\'' +
                ", elementId='" + getElementId() + '\'' +
                ", planItemDefinitionId='" + getPlanItemDefinitionId() + '\'' +
                ", stageInstanceId='" + getStageInstanceId() + '\'' +
                '}';
    }
}
