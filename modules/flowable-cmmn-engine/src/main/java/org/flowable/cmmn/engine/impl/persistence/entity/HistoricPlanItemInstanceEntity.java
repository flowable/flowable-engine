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

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.common.engine.impl.persistence.entity.Entity;

import java.util.Date;

/**
 * @author Dennis Federico
 */
public interface HistoricPlanItemInstanceEntity extends Entity, HistoricPlanItemInstance {

    void setId(String id);

    void setName(String name);

    void setState(String state);

    void setCaseDefinitionId(String caseDefinitionId);

    void setCaseInstanceId(String caseInstanceId);

    void setStageInstanceId(String stageInstanceId);

    void setStage(boolean isStage);

    void setElementId(String elementId);

    void setPlanItemDefinitionId(String planItemDefinitionId);

    void setPlanItemDefinitionType(String planItemDefinitionType);

    void setStartTime(Date startTime);

    void setActivationTime(Date activationTime);

    void setEndTime(Date endTime);

    void setStartUserId(String startUserId);

    void setReferenceId(String referenceId);

    void setReferenceType(String referenceType);

    void setTenantId(String tenantId);

}
