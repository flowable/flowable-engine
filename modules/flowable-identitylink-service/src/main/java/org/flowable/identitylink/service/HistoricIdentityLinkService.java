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
package org.flowable.identitylink.service;

import java.util.List;

import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;

/**
 * Service which provides access to historic variables.
 * 
 * @author Tijs Rademakers
 */
public interface HistoricIdentityLinkService {
    
    HistoricIdentityLinkEntity getHistoricIdentityLink(String id);
    
    List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByTaskId(String taskId);
    
    List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByProcessInstanceId(String processInstanceId);
    
    List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType);

    HistoricIdentityLinkEntity createHistoricIdentityLink();
    
    void insertHistoricIdentityLink(HistoricIdentityLinkEntity identityLink, boolean fireCreateEvent);
    
    void deleteHistoricIdentityLink(String id);
    
    void deleteHistoricIdentityLink(HistoricIdentityLinkEntity identityLink);
    
    void deleteHistoricIdentityLinksByProcessInstanceId(String processInstanceId);
    
    void deleteHistoricIdentityLinksByTaskId(String taskId);
    
    void deleteHistoricIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType);
}
