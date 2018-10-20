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
package org.flowable.entitylink.service.impl;

import java.util.List;

import org.flowable.common.engine.impl.service.CommonServiceImpl;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkService;
import org.flowable.entitylink.service.EntityLinkServiceConfiguration;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntityManager;

/**
 * @author Tijs Rademakers
 */
public class EntityLinkServiceImpl extends CommonServiceImpl<EntityLinkServiceConfiguration> implements EntityLinkService {

    public EntityLinkServiceImpl(EntityLinkServiceConfiguration entityLinkServiceConfiguration) {
        super(entityLinkServiceConfiguration);
    }
    
    @Override
    public EntityLinkEntity getEntityLink(String id) {
        return getEntityLinkEntityManager().findById(id);
    }
    
    @Override
    public List<EntityLink> findEntityLinksByScopeIdAndType(String scopeId, String scopeType, String linkType) {
        return getEntityLinkEntityManager().findEntityLinksByScopeIdAndType(scopeId, scopeType, linkType);
    }
    
    @Override
    public List<EntityLink> findEntityLinksByReferenceScopeIdAndType(String referenceScopeId, String scopeType, String linkType) {
        return getEntityLinkEntityManager().findEntityLinksByReferenceScopeIdAndType(referenceScopeId, scopeType, linkType);
    }
    
    @Override
    public List<EntityLink> findEntityLinksByScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType, String linkType) {
        return getEntityLinkEntityManager().findEntityLinksByScopeDefinitionIdAndType(scopeDefinitionId, scopeType, linkType);
    }
    
    @Override
    public EntityLinkEntity createEntityLink() {
        return getEntityLinkEntityManager().create();
    }
    
    @Override
    public void insertEntityLink(EntityLink entityLink) {
        getEntityLinkEntityManager().insert((EntityLinkEntity) entityLink);
    }
    
    @Override
    public void deleteEntityLink(EntityLink entityLink) {
        getEntityLinkEntityManager().delete((EntityLinkEntity) entityLink);
    }
    
    @Override
    public List<EntityLink> deleteScopeEntityLink(String scopeId, String scopeType, String linkType) {
        return getEntityLinkEntityManager().deleteScopeEntityLink(scopeId, scopeType, linkType);
    }
    
    @Override
    public List<EntityLink> deleteScopeDefinitionEntityLink(String scopeDefinitionId, String scopeType, String linkType) {
        return getEntityLinkEntityManager().deleteScopeDefinitionEntityLink(scopeDefinitionId, scopeType, linkType);
    }
    
    @Override
    public void deleteEntityLinksByScopeIdAndType(String scopeId, String scopeType) {
        getEntityLinkEntityManager().deleteEntityLinksByScopeIdAndScopeType(scopeId, scopeType);
    }
    
    @Override
    public void deleteEntityLinksByScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType) {
        getEntityLinkEntityManager().deleteEntityLinksByScopeDefinitionIdAndScopeType(scopeDefinitionId, scopeType);
    }
    
    public EntityLinkEntityManager getEntityLinkEntityManager() {
        return configuration.getEntityLinkEntityManager();
    }
}
