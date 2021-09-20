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

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public interface CaseInstanceEntity extends Entity, EntityWithSentryPartInstances, VariableScope, HasRevision, PlanItemInstanceContainer, CaseInstance {

    void setBusinessKey(String businessKey);
    void setBusinessStatus(String businessStatus);
    void setName(String name);
    void setParentId(String parentId);
    void setCaseDefinitionId(String caseDefinitionId);
    void setCaseDefinitionKey(String caseDefinitionKey);
    void setCaseDefinitionName(String caseDefinitionName);
    void setCaseDefinitionVersion(Integer caseDefinitionVersion);
    void setCaseDefinitionDeploymentId(String caseDefinitionDeploymentId);
    void setState(String state);
    void setStartTime(Date startTime);
    void setStartUserId(String startUserId);
    void setLastReactivationTime(Date lastReactivationTime);
    void setLastReactivationUserId(String lastReactivationUserId);
    void setCallbackId(String callbackId);
    void setCallbackType(String callbackType);
    void setReferenceId(String referenceId);
    void setReferenceType(String referenceType);
    void setCompletable(boolean completable);
    void setTenantId(String tenantId);

    Date getLockTime();
    void setLockTime(Date lockTime);

    String getLockOwner();
    void setLockOwner(String lockOwner);

    List<VariableInstanceEntity> getQueryVariables();
}
