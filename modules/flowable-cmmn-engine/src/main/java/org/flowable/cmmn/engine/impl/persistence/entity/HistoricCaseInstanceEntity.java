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

import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public interface HistoricCaseInstanceEntity extends Entity, HasRevision, HistoricCaseInstance {

    void setBusinessKey(String businessKey);
    void setName(String name);
    void setParentId(String parentId);
    void setCaseDefinitionId(String caseDefinitionId);
    void setCaseDefinitionKey(String caseDefinitionKey);
    void setCaseDefinitionName(String caseDefinitionName);
    void setCaseDefinitionVersion(Integer caseDefinitionVersion);
    void setCaseDefinitionDeploymentId(String caseDefinitionDeploymentId);
    void setState(String state);
    void setStartTime(Date startTime);
    void setEndTime(Date endTime);
    void setStartUserId(String startUserId);
    void setCallbackId(String callbackId);
    void setCallbackType(String callbackType);
    void setReferenceId(String referenceId);
    void setReferenceType(String referenceType);
    void setTenantId(String tenantId);

    List<HistoricVariableInstanceEntity> getQueryVariables();
}
