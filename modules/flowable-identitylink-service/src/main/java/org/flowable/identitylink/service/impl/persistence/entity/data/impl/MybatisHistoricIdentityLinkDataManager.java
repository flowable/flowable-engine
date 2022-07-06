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
package org.flowable.identitylink.service.impl.persistence.entity.data.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntityImpl;
import org.flowable.identitylink.service.impl.persistence.entity.data.HistoricIdentityLinkDataManager;
import org.flowable.identitylink.service.impl.persistence.entity.data.impl.cachematcher.HistoricIdentityLinksByProcInstMatcher;
import org.flowable.identitylink.service.impl.persistence.entity.data.impl.cachematcher.HistoricIdentityLinksByScopeIdAndTypeMatcher;
import org.flowable.identitylink.service.impl.persistence.entity.data.impl.cachematcher.HistoricIdentityLinksBySubScopeIdAndTypeMatcher;

/**
 * @author Joram Barrez
 */
public class MybatisHistoricIdentityLinkDataManager extends AbstractDataManager<HistoricIdentityLinkEntity> implements HistoricIdentityLinkDataManager {

    protected CachedEntityMatcher<HistoricIdentityLinkEntity> historicIdentityLinksByProcInstMatcher = new HistoricIdentityLinksByProcInstMatcher();
    protected CachedEntityMatcher<HistoricIdentityLinkEntity> historicIdentityLinksByScopeIdAndTypeMatcher = new HistoricIdentityLinksByScopeIdAndTypeMatcher();
    protected CachedEntityMatcher<HistoricIdentityLinkEntity> historicIdentityLinksBySubScopeIdAndTypeMatcher = new HistoricIdentityLinksBySubScopeIdAndTypeMatcher();

    protected IdentityLinkServiceConfiguration identityLinkServiceConfiguration;
    
    public MybatisHistoricIdentityLinkDataManager(IdentityLinkServiceConfiguration identityLinkServiceConfiguration) {
        this.identityLinkServiceConfiguration = identityLinkServiceConfiguration;
    }
    
    @Override
    public Class<? extends HistoricIdentityLinkEntity> getManagedEntityClass() {
        return HistoricIdentityLinkEntityImpl.class;
    }

    @Override
    public HistoricIdentityLinkEntity create() {
        return new HistoricIdentityLinkEntityImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByTaskId(String taskId) {
        return getDbSqlSession().selectList("selectHistoricIdentityLinksByTask", taskId);
    }

    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByProcessInstanceId(final String processInstanceId) {
        return getList("selectHistoricIdentityLinksByProcessInstance", processInstanceId, historicIdentityLinksByProcInstMatcher, true);
    }

    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        parameters.put("scopeType", scopeType);
        return getList("selectHistoricIdentityLinksByScopeIdAndType", parameters, historicIdentityLinksByScopeIdAndTypeMatcher, true);
    }
    
    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksBySubScopeIdAndScopeType(String subScopeId, String scopeType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("subScopeId", subScopeId);
        parameters.put("scopeType", scopeType);
        return getList("selectHistoricIdentityLinksBySubScopeIdAndType", parameters, historicIdentityLinksBySubScopeIdAndTypeMatcher, true);
    }
    
    @Override
    public void deleteHistoricIdentityLinksByScopeIdAndType(String scopeId, String scopeType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeId", scopeId);
        parameters.put("scopeType", scopeType);
        getDbSqlSession().delete("deleteHistoricIdentityLinksByScopeIdAndScopeType", parameters, HistoricIdentityLinkEntityImpl.class);
    }
    
    @Override
    public void deleteHistoricIdentityLinksByScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scopeDefinitionId", scopeDefinitionId);
        parameters.put("scopeType", scopeType);
        getDbSqlSession().delete("deleteHistoricIdentityLinksByScopeDefinitionIdAndScopeType", parameters, HistoricIdentityLinkEntityImpl.class);
    }
    
    @Override
    public void bulkDeleteHistoricIdentityLinksForProcessInstanceIds(Collection<String> processInstanceIds) {
        getDbSqlSession().delete("bulkDeleteHistoricIdentityLinksForProcessInstanceIds", createSafeInValuesList(processInstanceIds), HistoricIdentityLinkEntityImpl.class);
    }
    
    @Override
    public void bulkDeleteHistoricIdentityLinksForTaskIds(Collection<String> taskIds) {
        getDbSqlSession().delete("bulkDeleteHistoricIdentityLinksForTaskIds", createSafeInValuesList(taskIds), HistoricIdentityLinkEntityImpl.class);
    }

    @Override
    public void bulkDeleteHistoricIdentityLinksForScopeIdsAndScopeType(Collection<String> scopeIds, String scopeType) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("scopeIds", createSafeInValuesList(scopeIds));
        parameters.put("scopeType", scopeType);
        
        getDbSqlSession().delete("bulkDeleteHistoricIdentityLinksForScopeIdsAndScopeType", parameters, HistoricIdentityLinkEntityImpl.class);
    }

    @Override
    public void deleteHistoricProcessIdentityLinksForNonExistingInstances() {
        getDbSqlSession().delete("bulkDeleteHistoricProcessIdentityLinks", null, HistoricIdentityLinkEntityImpl.class);
    }
    
    @Override
    public void deleteHistoricCaseIdentityLinksForNonExistingInstances() {
        getDbSqlSession().delete("bulkDeleteHistoricCaseIdentityLinks", null, HistoricIdentityLinkEntityImpl.class);
    }
    
    @Override
    public void deleteHistoricTaskIdentityLinksForNonExistingInstances() {
        getDbSqlSession().delete("bulkDeleteHistoricTaskIdentityLinks", null, HistoricIdentityLinkEntityImpl.class);
    }

    @Override
    protected IdGenerator getIdGenerator() {
        return identityLinkServiceConfiguration.getIdGenerator();
    }
    
}
