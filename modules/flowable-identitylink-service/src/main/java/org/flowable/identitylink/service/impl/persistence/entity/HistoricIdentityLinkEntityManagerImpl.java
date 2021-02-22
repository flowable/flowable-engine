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

import org.flowable.common.engine.impl.persistence.entity.AbstractServiceEngineEntityManager;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.persistence.entity.data.HistoricIdentityLinkDataManager;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class HistoricIdentityLinkEntityManagerImpl
    extends AbstractServiceEngineEntityManager<IdentityLinkServiceConfiguration, HistoricIdentityLinkEntity, HistoricIdentityLinkDataManager>
    implements HistoricIdentityLinkEntityManager {


    public HistoricIdentityLinkEntityManagerImpl(IdentityLinkServiceConfiguration identityLinkServiceConfiguration, HistoricIdentityLinkDataManager historicIdentityLinkDataManager) {
        super(identityLinkServiceConfiguration, identityLinkServiceConfiguration.getEngineName(), historicIdentityLinkDataManager);
    }

    @Override
    public HistoricIdentityLinkEntity create() {
        HistoricIdentityLinkEntity identityLinkEntity = super.create();
        identityLinkEntity.setCreateTime(getClock().getCurrentTime());
        return identityLinkEntity;
    }

    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByTaskId(String taskId) {
        return dataManager.findHistoricIdentityLinksByTaskId(taskId);
    }

    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByProcessInstanceId(String processInstanceId) {
        return dataManager.findHistoricIdentityLinksByProcessInstanceId(processInstanceId);
    }
    
    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType) {
        return dataManager.findHistoricIdentityLinksByScopeIdAndScopeType(scopeId, scopeType);
    }
    
    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksBySubScopeIdAndScopeType(String subScopeId, String scopeType) {
        return dataManager.findHistoricIdentityLinksBySubScopeIdAndScopeType(subScopeId, scopeType);
    }

    @Override
    public void deleteHistoricIdentityLinksByTaskId(String taskId) {
        List<HistoricIdentityLinkEntity> identityLinks = findHistoricIdentityLinksByTaskId(taskId);
        for (HistoricIdentityLinkEntity identityLink : identityLinks) {
            delete(identityLink);
        }
    }

    @Override
    public void deleteHistoricIdentityLinksByProcInstance(String processInstanceId) {
        List<HistoricIdentityLinkEntity> identityLinks = dataManager
                .findHistoricIdentityLinksByProcessInstanceId(processInstanceId);

        for (HistoricIdentityLinkEntity identityLink : identityLinks) {
            delete(identityLink);
        }
    }
    
    @Override
    public void deleteHistoricIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType) {
        dataManager.deleteHistoricIdentityLinksByScopeIdAndType(scopeId, scopeType);
    }
    
    @Override
    public void deleteHistoricIdentityLinksByScopeDefinitionIdAndScopeType(String scopeDefinitionId, String scopeType) {
        dataManager.deleteHistoricIdentityLinksByScopeDefinitionIdAndType(scopeDefinitionId, scopeType);
    }

    @Override
    public void deleteHistoricProcessIdentityLinksForNonExistingInstances() {
        dataManager.deleteHistoricProcessIdentityLinksForNonExistingInstances();
    }
    
    @Override
    public void deleteHistoricCaseIdentityLinksForNonExistingInstances() {
        dataManager.deleteHistoricCaseIdentityLinksForNonExistingInstances();
    }
    
    @Override
    public void deleteHistoricTaskIdentityLinksForNonExistingInstances() {
        dataManager.deleteHistoricTaskIdentityLinksForNonExistingInstances();
    }

}
