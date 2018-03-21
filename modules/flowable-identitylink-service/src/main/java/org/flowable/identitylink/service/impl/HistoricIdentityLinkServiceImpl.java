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
package org.flowable.identitylink.service.impl;

import java.util.List;

import org.flowable.identitylink.service.HistoricIdentityLinkService;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricIdentityLinkServiceImpl extends ServiceImpl implements HistoricIdentityLinkService {

    public HistoricIdentityLinkServiceImpl() {

    }

    public HistoricIdentityLinkServiceImpl(IdentityLinkServiceConfiguration identityLinkServiceConfiguration) {
        super(identityLinkServiceConfiguration);
    }
    
    @Override
    public HistoricIdentityLinkEntity getHistoricIdentityLink(String id) {
        return getHistoricIdentityLinkEntityManager().findById(id);
    }
    
    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByTaskId(String taskId) {
        return getHistoricIdentityLinkEntityManager().findHistoricIdentityLinksByTaskId(taskId);
    }
    
    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByProcessInstanceId(String processInstanceId) {
        return getHistoricIdentityLinkEntityManager().findHistoricIdentityLinksByProcessInstanceId(processInstanceId);
    }
    
    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType) {
        return getHistoricIdentityLinkEntityManager().findHistoricIdentityLinksByScopeIdAndScopeType(scopeId, scopeType);
    }
    
    @Override
    public HistoricIdentityLinkEntity createHistoricIdentityLink() {
        return getHistoricIdentityLinkEntityManager().create();
    }
    
    @Override
    public void insertHistoricIdentityLink(HistoricIdentityLinkEntity identityLink, boolean fireCreateEvent) {
        getHistoricIdentityLinkEntityManager().insert(identityLink, fireCreateEvent);
    }
    
    @Override
    public void deleteHistoricIdentityLink(String id) {
        getHistoricIdentityLinkEntityManager().delete(id);
    }
    
    @Override
    public void deleteHistoricIdentityLink(HistoricIdentityLinkEntity identityLink) {
        getHistoricIdentityLinkEntityManager().delete(identityLink);
    }
    
    @Override
    public void deleteHistoricIdentityLinksByProcessInstanceId(String processInstanceId) {
        getHistoricIdentityLinkEntityManager().deleteHistoricIdentityLinksByProcInstance(processInstanceId);
    }

    @Override
    public void deleteHistoricIdentityLinksByTaskId(String taskId) {
        getHistoricIdentityLinkEntityManager().deleteHistoricIdentityLinksByTaskId(taskId);
    }
    
    @Override
    public void deleteHistoricIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType) {
        getHistoricIdentityLinkEntityManager().deleteHistoricIdentityLinksByScopeIdAndScopeType(scopeId, scopeType);
    }
}
