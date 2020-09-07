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
package org.flowable.variable.service.impl.persistence.entity.data.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.InternalVariableInstanceQueryImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.data.VariableInstanceDataManager;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.VariableInstanceByExecutionIdMatcher;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.VariableInstanceByScopeIdAndScopeTypeMatcher;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.VariableInstanceByScopeIdAndScopeTypesMatcher;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.VariableInstanceBySubScopeIdAndScopeTypesMatcher;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.VariableInstanceByTaskIdMatcher;

/**
 * @author Joram Barrez
 */
public class MybatisVariableInstanceDataManager extends AbstractDataManager<VariableInstanceEntity> implements VariableInstanceDataManager {

    protected CachedEntityMatcher<VariableInstanceEntity> variableInstanceByExecutionIdMatcher 
        = new VariableInstanceByExecutionIdMatcher();

    protected CachedEntityMatcher<VariableInstanceEntity> variableInstanceByTaskIdMatcher
        = new VariableInstanceByTaskIdMatcher();
    
    protected CachedEntityMatcher<VariableInstanceEntity> variableInstanceByScopeIdAndScopeTypeMatcher 
        = new VariableInstanceByScopeIdAndScopeTypeMatcher();

    protected CachedEntityMatcher<VariableInstanceEntity> variableInstanceByScopeIdAndScopeTypesMatcher
        = new VariableInstanceByScopeIdAndScopeTypesMatcher();

    protected CachedEntityMatcher<VariableInstanceEntity> variableInstanceBySubScopeIdAndScopeTypesMatcher
        = new VariableInstanceBySubScopeIdAndScopeTypesMatcher();
    
    protected VariableServiceConfiguration variableServiceConfiguration;
    
    public MybatisVariableInstanceDataManager(VariableServiceConfiguration variableServiceConfiguration) {
        this.variableServiceConfiguration = variableServiceConfiguration;
    }

    @Override
    public Class<? extends VariableInstanceEntity> getManagedEntityClass() {
        return VariableInstanceEntityImpl.class;
    }

    @Override
    public VariableInstanceEntity create() {
        VariableInstanceEntityImpl variableInstanceEntity = new VariableInstanceEntityImpl();
        variableInstanceEntity.setRevision(0); // For backwards compatibility, variables / HistoricVariableUpdate assumes revision 0 for the first time
        return variableInstanceEntity;
    }

    @Override
    public List<VariableInstanceEntity> findVariablesInstancesByQuery(InternalVariableInstanceQueryImpl internalVariableInstanceQuery) {
        return getList("selectVariablesByQuery", internalVariableInstanceQuery, internalVariableInstanceQuery, true);
    }

    @Override
    public VariableInstanceEntity findVariablesInstanceByQuery(InternalVariableInstanceQueryImpl internalVariableInstanceQuery) {
        return getEntity("selectVariablesByQuery", internalVariableInstanceQuery, internalVariableInstanceQuery, true);
    }

    @Override
    public void deleteVariablesByTaskId(String taskId) {
        DbSqlSession dbSqlSession = getDbSqlSession();
        if (isEntityInserted(dbSqlSession, "task", taskId)) {
            deleteCachedEntities(dbSqlSession, variableInstanceByTaskIdMatcher, taskId);
        } else {
            bulkDelete("deleteVariableInstancesByTaskId", variableInstanceByTaskIdMatcher, taskId);
        }
    }
    
    @Override
    public void deleteVariablesByExecutionId(String executionId) {
        DbSqlSession dbSqlSession = getDbSqlSession();
        if (isEntityInserted(dbSqlSession, "execution", executionId)) {
            deleteCachedEntities(dbSqlSession, variableInstanceByExecutionIdMatcher, executionId);
        } else {
            bulkDelete("deleteVariableInstancesByExecutionId", variableInstanceByExecutionIdMatcher, executionId);
        }
    }
    
    @Override
    public void deleteByScopeIdAndScopeType(String scopeId, String scopeType) {
        Map<String, Object> params = new HashMap<>(3);
        params.put("scopeId", scopeId);
        params.put("scopeType", scopeType);
        bulkDelete("deleteVariablesByScopeIdAndScopeType", variableInstanceByScopeIdAndScopeTypeMatcher, params);
    }

    @Override
    public void deleteByScopeIdAndScopeTypes(String scopeId, Collection<String> scopeTypes) {
        if (scopeTypes.size() == 1) {
            deleteByScopeIdAndScopeType(scopeId, scopeTypes.iterator().next());
            return;
        }

        Map<String, Object> params = new HashMap<>(3);
        params.put("scopeId", scopeId);
        params.put("scopeTypes", scopeTypes);
        bulkDelete("deleteVariablesByScopeIdAndScopeTypes", variableInstanceByScopeIdAndScopeTypesMatcher, params);

    }

    @Override
    public void deleteBySubScopeIdAndScopeTypes(String subScopeId, Collection<String> scopeTypes) {
        Map<String, Object> params = new HashMap<>(3);
        params.put("subScopeId", subScopeId);
        params.put("scopeTypes", scopeTypes);
        bulkDelete("deleteVariablesBySubScopeIdAndScopeTypes", variableInstanceBySubScopeIdAndScopeTypesMatcher, params);
    }
    
    @Override
    protected IdGenerator getIdGenerator() {
        return variableServiceConfiguration.getIdGenerator();
    }
}
