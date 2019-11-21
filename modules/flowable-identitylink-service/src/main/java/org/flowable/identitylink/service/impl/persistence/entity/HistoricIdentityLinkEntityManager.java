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
package org.flowable.identitylink.service.impl.persistence.entity;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.EntityManager;

/**
 * @author Joram Barrez
 */
public interface HistoricIdentityLinkEntityManager extends EntityManager<HistoricIdentityLinkEntity> {

    List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByTaskId(String taskId);

    List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByProcessInstanceId(String processInstanceId);
    
    List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType);
    
    List<HistoricIdentityLinkEntity> findHistoricIdentityLinksBySubScopeIdAndScopeType(String subScopeId, String scopeType);

    void deleteHistoricIdentityLinksByTaskId(String taskId);

    void deleteHistoricIdentityLinksByProcInstance(String processInstanceId);
    
    void deleteHistoricIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType);
    
    void deleteHistoricIdentityLinksByScopeDefinitionIdAndScopeType(String scopeDefinitionId, String scopeType);
    
    void deleteHistoricProcessIdentityLinksForNonExistingInstances();
    
    void deleteHistoricCaseIdentityLinksForNonExistingInstances();
    
    void deleteHistoricTaskIdentityLinksForNonExistingInstances();

}