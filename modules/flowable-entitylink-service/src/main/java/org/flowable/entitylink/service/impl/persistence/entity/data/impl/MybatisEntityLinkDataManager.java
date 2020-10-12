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
package org.flowable.entitylink.service.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.service.EntityLinkServiceConfiguration;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntityImpl;
import org.flowable.entitylink.service.impl.persistence.entity.data.EntityLinkDataManager;
import org.flowable.entitylink.service.impl.persistence.entity.data.impl.cachematcher.EntityLinksByReferenceScopeIdAndTypeMatcher;
import org.flowable.entitylink.service.impl.persistence.entity.data.impl.cachematcher.EntityLinksByRootScopeIdAndTypeMatcher;
import org.flowable.entitylink.service.impl.persistence.entity.data.impl.cachematcher.EntityLinksByScopeIdAndTypeMatcher;
import org.flowable.entitylink.service.impl.persistence.entity.data.impl.cachematcher.EntityLinksWithSameRootScopeForScopeIdAndScopeTypeMatcher;

/**
 * @author Tijs Rademakers
 */
public class MybatisEntityLinkDataManager extends AbstractDataManager<EntityLinkEntity> implements EntityLinkDataManager {

    protected CachedEntityMatcher<EntityLinkEntity> entityLinksByScopeIdAndTypeMatcher = new EntityLinksByScopeIdAndTypeMatcher();
    protected CachedEntityMatcher<EntityLinkEntity> entityLinksByRootScopeIdAndScopeTypeMatcher = new EntityLinksByRootScopeIdAndTypeMatcher();
    protected CachedEntityMatcher<EntityLinkEntity> entityLinksWithSameRootByScopeIdAndTypeMatcher = new EntityLinksWithSameRootScopeForScopeIdAndScopeTypeMatcher<>();
    protected CachedEntityMatcher<EntityLinkEntity> entityLinksByReferenceScopeIdAndTypeMatcher = new EntityLinksByReferenceScopeIdAndTypeMatcher();

    protected EntityLinkServiceConfiguration entityLinkServiceConfiguration;
    
    public MybatisEntityLinkDataManager(EntityLinkServiceConfiguration entityLinkServiceConfiguration) {
        this.entityLinkServiceConfiguration = entityLinkServiceConfiguration;
    }
    
    @Override
    public Class<? extends EntityLinkEntity> getManagedEntityClass() {
        return EntityLinkEntityImpl.class;
    }

    @Override
    public EntityLinkEntity create() {
        return new EntityLinkEntityImpl();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<EntityLink> findEntityLinksByScopeIdAndType(String scopeId, String scopeType, String linkType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        parameters.put("scopeType", scopeType);
        parameters.put("linkType", linkType);
        return (List) getList("selectEntityLinksByScopeIdAndType", parameters, entityLinksByScopeIdAndTypeMatcher, true);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<EntityLink> findEntityLinksByRootScopeIdAndRootType(String scopeId, String scopeType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("rootScopeId", scopeId);
        parameters.put("rootScopeType", scopeType);
        return (List) getList("selectEntityLinksByRootScopeIdAndRootScopeType", parameters, entityLinksByRootScopeIdAndScopeTypeMatcher, true);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<EntityLink> findEntityLinksWithSameRootScopeForScopeIdAndScopeType(String scopeId, String scopeType, String linkType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        parameters.put("scopeType", scopeType);
        parameters.put("linkType", linkType);
        return (List) getList("selectEntityLinksWithSameRootScopeByScopeIdAndType", parameters, entityLinksWithSameRootByScopeIdAndTypeMatcher, true);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<EntityLink> findEntityLinksByReferenceScopeIdAndType(String referenceScopeId, String referenceScopeType, String linkType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("referenceScopeId", referenceScopeId);
        parameters.put("referenceScopeType", referenceScopeType);
        parameters.put("linkType", linkType);
        return (List) getList("selectEntityLinksByReferenceScopeIdAndType", parameters, entityLinksByReferenceScopeIdAndTypeMatcher, true);
    }

    @Override
    public void deleteEntityLinksByScopeIdAndScopeType(String scopeId, String scopeType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        parameters.put("scopeType", scopeType);
        bulkDelete("deleteEntityLinksByScopeIdAndScopeType", entityLinksByScopeIdAndTypeMatcher, parameters);
    }

    @Override
    public void deleteEntityLinksByRootScopeIdAndType(String scopeId, String scopeType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("rootScopeId", scopeId);
        parameters.put("rootScopeType", scopeType);
        bulkDelete("deleteEntityLinksByRootScopeIdAndRootScopeType", entityLinksByRootScopeIdAndScopeTypeMatcher, parameters);
    }

    @Override
    protected IdGenerator getIdGenerator() {
        return entityLinkServiceConfiguration.getIdGenerator();
    }
    
}
