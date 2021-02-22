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

package org.flowable.entitylink.service.impl.persistence.entity;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.AbstractServiceEngineEntityManager;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.entitylink.service.EntityLinkServiceConfiguration;
import org.flowable.entitylink.service.impl.persistence.entity.data.HistoricEntityLinkDataManager;

/**
 * @author Tijs Rademakers
 */
public class HistoricEntityLinkEntityManagerImpl
    extends AbstractServiceEngineEntityManager<EntityLinkServiceConfiguration, HistoricEntityLinkEntity, HistoricEntityLinkDataManager>
    implements HistoricEntityLinkEntityManager {

    public HistoricEntityLinkEntityManagerImpl(EntityLinkServiceConfiguration entityLinkServiceConfiguration, HistoricEntityLinkDataManager historicEntityLinkDataManager) {
        super(entityLinkServiceConfiguration, entityLinkServiceConfiguration.getEngineName(), historicEntityLinkDataManager);
    }

    @Override
    public HistoricEntityLinkEntity create() {
        HistoricEntityLinkEntity entityLinkEntity = super.create();
        entityLinkEntity.setCreateTime(getClock().getCurrentTime());
        return entityLinkEntity;
    }

    @Override
    public List<HistoricEntityLink> findHistoricEntityLinksByScopeIdAndScopeType(String scopeId, String scopeType, String linkType) {
        return dataManager.findHistoricEntityLinksByScopeIdAndScopeType(scopeId, scopeType, linkType);
    }

    @Override
    public List<HistoricEntityLink> findHistoricEntityLinksWithSameRootScopeForScopeIdAndScopeType(String scopeId, String scopeType, String linkType) {
        return dataManager.findHistoricEntityLinksWithSameRootScopeForScopeIdAndScopeType(scopeId, scopeType, linkType);
    }

    @Override
    public List<HistoricEntityLink> findHistoricEntityLinksByReferenceScopeIdAndType(String referenceScopeId, String scopeType, String linkType) {
        return dataManager.findHistoricEntityLinksByReferenceScopeIdAndType(referenceScopeId, scopeType, linkType);
    }
    
    @Override
    public List<HistoricEntityLink> findHistoricEntityLinksByScopeDefinitionIdAndScopeType(String scopeDefinitionId, String scopeType, String linkType) {
        return dataManager.findHistoricEntityLinksByScopeDefinitionIdAndScopeType(scopeDefinitionId, scopeType, linkType);
    }

    @Override
    public void deleteHistoricEntityLinksByScopeIdAndScopeType(String scopeId, String scopeType) {
        dataManager.deleteHistoricEntityLinksByScopeIdAndType(scopeId, scopeType);
    }
    
    @Override
    public void deleteHistoricEntityLinksByScopeDefinitionIdAndScopeType(String scopeDefinitionId, String scopeType) {
        dataManager.deleteHistoricEntityLinksByScopeDefinitionIdAndType(scopeDefinitionId, scopeType);
    }
    
    @Override
    public void deleteHistoricEntityLinksForNonExistingProcessInstances() {
        dataManager.deleteHistoricEntityLinksForNonExistingProcessInstances();
    }
    
    @Override
    public void deleteHistoricEntityLinksForNonExistingCaseInstances() {
        dataManager.deleteHistoricEntityLinksForNonExistingCaseInstances();
    }

}
