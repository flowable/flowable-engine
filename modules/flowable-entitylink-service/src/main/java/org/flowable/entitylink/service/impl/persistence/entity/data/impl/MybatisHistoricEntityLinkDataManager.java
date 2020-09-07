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
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.entitylink.service.EntityLinkServiceConfiguration;
import org.flowable.entitylink.service.impl.persistence.entity.HistoricEntityLinkEntity;
import org.flowable.entitylink.service.impl.persistence.entity.HistoricEntityLinkEntityImpl;
import org.flowable.entitylink.service.impl.persistence.entity.data.HistoricEntityLinkDataManager;
import org.flowable.entitylink.service.impl.persistence.entity.data.impl.cachematcher.EntityLinksWithSameRootScopeForScopeIdAndScopeTypeMatcher;
import org.flowable.entitylink.service.impl.persistence.entity.data.impl.cachematcher.HistoricEntityLinksByScopeIdAndTypeMatcher;

/**
 * @author Joram Barrez
 */
public class MybatisHistoricEntityLinkDataManager extends AbstractDataManager<HistoricEntityLinkEntity> implements HistoricEntityLinkDataManager {

    protected CachedEntityMatcher<HistoricEntityLinkEntity> historicEntityLinksByScopeIdAndTypeMatcher = new HistoricEntityLinksByScopeIdAndTypeMatcher();
    protected CachedEntityMatcher<HistoricEntityLinkEntity> entityLinksWithSameRootByScopeIdAndTypeMatcher = new EntityLinksWithSameRootScopeForScopeIdAndScopeTypeMatcher<>();

    protected EntityLinkServiceConfiguration entityLinkServiceConfiguration;
    
    public MybatisHistoricEntityLinkDataManager(EntityLinkServiceConfiguration entityLinkServiceConfiguration) {
        this.entityLinkServiceConfiguration = entityLinkServiceConfiguration;
    }
    
    @Override
    public Class<? extends HistoricEntityLinkEntity> getManagedEntityClass() {
        return HistoricEntityLinkEntityImpl.class;
    }

    @Override
    public HistoricEntityLinkEntity create() {
        return new HistoricEntityLinkEntityImpl();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<HistoricEntityLink> findHistoricEntityLinksByScopeIdAndScopeType(String scopeId, String scopeType, String linkType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        parameters.put("scopeType", scopeType);
        parameters.put("linkType", linkType);
        return (List) getList("selectHistoricEntityLinksByScopeIdAndType", parameters, historicEntityLinksByScopeIdAndTypeMatcher, true);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<HistoricEntityLink> findHistoricEntityLinksWithSameRootScopeForScopeIdAndScopeType(String scopeId, String scopeType, String linkType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        parameters.put("scopeType", scopeType);
        parameters.put("linkType", linkType);
        return (List) getList("selectHistoricEntityLinksWithSameRootScopeByScopeIdAndType", parameters, entityLinksWithSameRootByScopeIdAndTypeMatcher, true);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<HistoricEntityLink> findHistoricEntityLinksByReferenceScopeIdAndType(String referenceScopeId, String scopeType, String linkType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("referenceScopeId", referenceScopeId);
        parameters.put("referenceScopeType", scopeType);
        parameters.put("linkType", linkType);
        return (List) getList("selectHistoricEntityLinksByReferenceScopeIdAndType", parameters, historicEntityLinksByScopeIdAndTypeMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricEntityLink> findHistoricEntityLinksByScopeDefinitionIdAndScopeType(String scopeDefinitionId, String scopeType, String linkType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeDefinitionId", scopeDefinitionId);
        parameters.put("scopeType", scopeType);
        parameters.put("linkType", linkType);
        return getDbSqlSession().selectList("selectHistoricEntityLinksByScopeDefinitionIdAndType", parameters);
    }
    
    @Override
    public void deleteHistoricEntityLinksByScopeIdAndType(String scopeId, String scopeType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        parameters.put("scopeType", scopeType);
        getDbSqlSession().delete("deleteHistoricEntityLinksByScopeIdAndScopeType", parameters, HistoricEntityLinkEntityImpl.class);
    }
    
    @Override
    public void deleteHistoricEntityLinksByScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeDefinitionId", scopeDefinitionId);
        parameters.put("scopeType", scopeType);
        getDbSqlSession().delete("deleteHistoricEntityLinksByScopeDefinitionIdAndScopeType", parameters, HistoricEntityLinkEntityImpl.class);
    }
    
    @Override
    public void deleteHistoricEntityLinksForNonExistingProcessInstances() {
        getDbSqlSession().delete("bulkDeleteHistoricProcessEntityLinks", null, HistoricEntityLinkEntityImpl.class);
    }
    
    @Override
    public void deleteHistoricEntityLinksForNonExistingCaseInstances() {
        getDbSqlSession().delete("bulkDeleteHistoricCaseEntityLinks", null, HistoricEntityLinkEntityImpl.class);
    }

    @Override
    protected IdGenerator getIdGenerator() {
        return entityLinkServiceConfiguration.getIdGenerator();
    }
    
}
