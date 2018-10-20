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

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.entitylink.service.EntityLinkServiceConfiguration;
import org.flowable.entitylink.service.impl.persistence.entity.data.HistoricEntityLinkDataManager;

/**
 * @author Tijs Rademakers
 */
public class HistoricEntityLinkEntityManagerImpl extends AbstractEntityManager<HistoricEntityLinkEntity> implements HistoricEntityLinkEntityManager {

    protected HistoricEntityLinkDataManager historicEntityLinkDataManager;

    public HistoricEntityLinkEntityManagerImpl(EntityLinkServiceConfiguration identityLinkServiceConfiguration, HistoricEntityLinkDataManager historicEntityLinkDataManager) {
        super(identityLinkServiceConfiguration);
        this.historicEntityLinkDataManager = historicEntityLinkDataManager;
    }

    @Override
    public HistoricEntityLinkEntity create() {
        HistoricEntityLinkEntity entityLinkEntity = super.create();
        entityLinkEntity.setCreateTime(entityLinkServiceConfiguration.getClock().getCurrentTime());
        return entityLinkEntity;
    }

    @Override
    protected DataManager<HistoricEntityLinkEntity> getDataManager() {
        return historicEntityLinkDataManager;
    }

    @Override
    public List<HistoricEntityLink> findHistoricEntityLinksByScopeIdAndScopeType(String scopeId, String scopeType, String linkType) {
        return historicEntityLinkDataManager.findHistoricEntityLinksByScopeIdAndScopeType(scopeId, scopeType, linkType);
    }
    
    @Override
    public List<HistoricEntityLink> findHistoricEntityLinksByScopeDefinitionIdAndScopeType(String scopeDefinitionId, String scopeType, String linkType) {
        return historicEntityLinkDataManager.findHistoricEntityLinksByScopeDefinitionIdAndScopeType(scopeDefinitionId, scopeType, linkType);
    }

    @Override
    public void deleteHistoricEntityLinksByScopeIdAndScopeType(String scopeId, String scopeType) {
        historicEntityLinkDataManager.deleteHistoricEntityLinksByScopeIdAndType(scopeId, scopeType);
    }
    
    @Override
    public void deleteHistoricEntityLinksByScopeDefinitionIdAndScopeType(String scopeDefinitionId, String scopeType) {
        historicEntityLinkDataManager.deleteHistoricEntityLinksByScopeDefinitionIdAndType(scopeDefinitionId, scopeType);
    }

    public HistoricEntityLinkDataManager getHistoricEntityLinkDataManager() {
        return historicEntityLinkDataManager;
    }

    public void setHistoricEntityLinkDataManager(HistoricEntityLinkDataManager historicEntityLinkDataManager) {
        this.historicEntityLinkDataManager = historicEntityLinkDataManager;
    }

}
