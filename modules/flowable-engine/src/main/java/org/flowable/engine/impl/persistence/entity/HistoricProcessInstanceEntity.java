/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public interface HistoricProcessInstanceEntity extends HistoricScopeInstanceEntity, HistoricProcessInstance, HasRevision {

    void setEndActivityId(String endActivityId);

    void setBusinessKey(String businessKey);

    void setStartUserId(String startUserId);

    void setStartActivityId(String startUserId);

    void setSuperProcessInstanceId(String superProcessInstanceId);

    void setTenantId(String tenantId);

    void setName(String name);

    void setLocalizedName(String localizedName);

    void setDescription(String description);

    void setLocalizedDescription(String localizedDescription);

    void setProcessDefinitionKey(String processDefinitionKey);

    void setProcessDefinitionName(String processDefinitionName);

    void setProcessDefinitionVersion(Integer processDefinitionVersion);

    void setDeploymentId(String deploymentId);
    
    void setCallbackId(String callbackId);
    
    void setCallbackType(String callbackType);
    
    List<HistoricVariableInstanceEntity> getQueryVariables();

    void setQueryVariables(List<HistoricVariableInstanceEntity> queryVariables);

}
