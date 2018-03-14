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

import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;

/**
 * @author Dennis Federico
 */
public class UserEventListenerInstanceImpl implements UserEventListenerInstance {

    private String id;
    private String name;
    private String caseInstanceId;
    private String caseDefinitionId;
    private String elementId;
    private String planItemDefinitionId;
    private String stageIntanceId;
    private String state;

    private UserEventListenerInstanceImpl() {

    }

    protected static UserEventListenerInstance fromPlanItemInstance(PlanItemInstance planItemInstance) {
        if (planItemInstance == null) {
            return null;
        }
        UserEventListenerInstanceImpl instance = new UserEventListenerInstanceImpl();
        if (PlanItemDefinitionType.USER_EVENT_LISTENER.equals(planItemInstance.getPlanItemDefinitionType())) {
            instance.id = planItemInstance.getId();
            instance.name = planItemInstance.getName();
            instance.caseInstanceId = planItemInstance.getCaseInstanceId();
            instance.caseDefinitionId = planItemInstance.getCaseDefinitionId();
            instance.elementId = planItemInstance.getElementId();
            instance.planItemDefinitionId = planItemInstance.getPlanItemDefinitionId();
            instance.stageIntanceId = planItemInstance.getStageInstanceId();
            instance.state = planItemInstance.getState();
        }
        return instance;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    @Override
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    @Override
    public String getElementId() {
        return elementId;
    }

    @Override
    public String getPlanItemDefinitionId() {
        return planItemDefinitionId;
    }

    @Override
    public String getStageIntanceId() {
        return stageIntanceId;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public String toString() {
        return "UserEventListenerInstanceImpl{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", caseInstanceId='" + caseInstanceId + '\'' +
                ", caseDefinitionId='" + caseDefinitionId + '\'' +
                ", elementId='" + elementId + '\'' +
                ", planItemDefinitionId='" + planItemDefinitionId + '\'' +
                ", stageIntanceId='" + stageIntanceId + '\'' +
                '}';
    }
}
