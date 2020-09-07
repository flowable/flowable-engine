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
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.service.EntityLinkServiceConfiguration;
import org.flowable.entitylink.service.impl.persistence.entity.data.EntityLinkDataManager;

/**
 * @author Tijs Rademakers
 */
public class EntityLinkEntityManagerImpl
    extends AbstractServiceEngineEntityManager<EntityLinkServiceConfiguration, EntityLinkEntity, EntityLinkDataManager>
    implements EntityLinkEntityManager {

    public EntityLinkEntityManagerImpl(EntityLinkServiceConfiguration entityLinkServiceConfiguration, EntityLinkDataManager entityLinkDataManager) {
        super(entityLinkServiceConfiguration, entityLinkServiceConfiguration.getEngineName(), entityLinkDataManager);
    }
    
    @Override
    public EntityLinkEntity create() {
        EntityLinkEntity entityLinkEntity = super.create();
        entityLinkEntity.setCreateTime(getClock().getCurrentTime());
        return entityLinkEntity;
    }

    @Override
    public List<EntityLink> findEntityLinksByScopeIdAndType(String scopeId, String scopeType, String linkType) {
        return dataManager.findEntityLinksByScopeIdAndType(scopeId, scopeType, linkType);
    }

    @Override
    public List<EntityLink> findEntityLinksByRootScopeIdAndRootType(String scopeId, String scopeType) {
        return dataManager.findEntityLinksByRootScopeIdAndRootType(scopeId, scopeType);
    }

    @Override
    public List<EntityLink> findEntityLinksWithSameRootScopeForScopeIdAndScopeType(String scopeId, String scopeType, String linkType) {
        return dataManager.findEntityLinksWithSameRootScopeForScopeIdAndScopeType(scopeId, scopeType, linkType);
    }
    
    @Override
    public List<EntityLink> findEntityLinksByReferenceScopeIdAndType(String referenceScopeId, String referenceScopeType, String linkType) {
        return dataManager.findEntityLinksByReferenceScopeIdAndType(referenceScopeId, referenceScopeType, linkType);
    }

    public void deleteEntityLink(EntityLinkEntity identityLink) {
        delete(identityLink);
    }

    @Override
    public void deleteEntityLinksByScopeIdAndScopeType(String scopeId, String scopeType) {
        dataManager.deleteEntityLinksByScopeIdAndScopeType(scopeId, scopeType);
    }

    @Override
    public void deleteEntityLinksByRootScopeIdAndType(String scopeId, String scopeType) {
        dataManager.deleteEntityLinksByRootScopeIdAndType(scopeId, scopeType);
    }

}
