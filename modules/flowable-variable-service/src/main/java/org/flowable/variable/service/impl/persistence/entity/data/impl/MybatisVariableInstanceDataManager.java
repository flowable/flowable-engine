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
import java.util.Set;

import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.db.SingleCachedEntityMatcher;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.data.VariableInstanceDataManager;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.VariableInstanceByExecutionIdMatcher;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.VariableInstanceByScopeIdAndScopeTypeAndVariableNameMatcher;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.VariableInstanceByScopeIdAndScopeTypeAndVariableNamesMatcher;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.VariableInstanceByScopeIdAndScopeTypeMatcher;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.VariableInstanceBySubScopeIdAndScopeTypeAndVariableNameMatcher;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.VariableInstanceBySubScopeIdAndScopeTypeAndVariableNamesMatcher;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.VariableInstanceBySubScopeIdAndScopeTypeMatcher;
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
    protected SingleCachedEntityMatcher<VariableInstanceEntity> variableInstanceByScopeIdAndScopeTypeAndVariableNameMatcher 
        = new VariableInstanceByScopeIdAndScopeTypeAndVariableNameMatcher();
    protected CachedEntityMatcher<VariableInstanceEntity> variableInstanceByScopeIdAndScopeTypeAndVariableNamesMatcher 
        = new VariableInstanceByScopeIdAndScopeTypeAndVariableNamesMatcher();
    
    protected CachedEntityMatcher<VariableInstanceEntity> variableInstanceBySubScopeIdAndScopeTypeMatcher 
        = new VariableInstanceBySubScopeIdAndScopeTypeMatcher();
    protected SingleCachedEntityMatcher<VariableInstanceEntity> variableInstanceBySubScopeIdAndScopeTypeAndVariableNameMatcher 
        = new VariableInstanceBySubScopeIdAndScopeTypeAndVariableNameMatcher();
    protected CachedEntityMatcher<VariableInstanceEntity> variableInstanceBySubScopeIdAndScopeTypeAndVariableNamesMatcher 
        = new VariableInstanceBySubScopeIdAndScopeTypeAndVariableNamesMatcher();

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
    public List<VariableInstanceEntity> findVariableInstancesByTaskId(String taskId) {
        return getList("selectVariablesByTaskId", taskId, variableInstanceByTaskIdMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VariableInstanceEntity> findVariableInstancesByTaskIds(Set<String> taskIds) {
        return getDbSqlSession().selectList("selectVariablesByTaskIds", taskIds);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionId(final String executionId) {
        return getList("selectVariablesByExecutionId", executionId, variableInstanceByExecutionIdMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VariableInstanceEntity> findVariableInstancesByExecutionIds(Set<String> executionIds) {
        return getDbSqlSession().selectList("selectVariablesByExecutionIds", executionIds);
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByExecutionAndName(String executionId, String variableName) {
        Map<String, String> params = new HashMap<>(2);
        params.put("executionId", executionId);
        params.put("name", variableName);
        return (VariableInstanceEntity) getDbSqlSession().selectOne("selectVariableInstanceByExecutionAndName", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VariableInstanceEntity> findVariableInstancesByExecutionAndNames(String executionId, Collection<String> names) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("executionId", executionId);
        params.put("names", names);
        return getDbSqlSession().selectList("selectVariableInstancesByExecutionAndNames", params);
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByTaskAndName(String taskId, String variableName) {
        Map<String, String> params = new HashMap<>(2);
        params.put("taskId", taskId);
        params.put("name", variableName);
        return (VariableInstanceEntity) getDbSqlSession().selectOne("selectVariableInstanceByTaskAndName", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VariableInstanceEntity> findVariableInstancesByTaskAndNames(String taskId, Collection<String> names) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("taskId", taskId);
        params.put("names", names);
        return getDbSqlSession().selectList("selectVariableInstancesByTaskAndNames", params);
    }
    
    @Override
    public List<VariableInstanceEntity> findVariableInstanceByScopeIdAndScopeType(String scopeId, String scopeType) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("scopeId", scopeId);
        params.put("scopeType", scopeType);
        return getList("selectVariableInstancesByScopeIdAndScopeType", params, variableInstanceByScopeIdAndScopeTypeMatcher, true); 
    }
    
    @Override
    public VariableInstanceEntity findVariableInstanceByScopeIdAndScopeTypeAndName(String scopeId, String scopeType, String variableName) {
        Map<String, String> params = new HashMap<>(3);
        params.put("scopeId", scopeId);
        params.put("scopeType", scopeType);
        params.put("variableName", variableName);
        return getEntity("selectVariableInstanceByScopeIdAndScopeTypeAndName", params, variableInstanceByScopeIdAndScopeTypeAndVariableNameMatcher, true);
    }
    
    @Override
    public List<VariableInstanceEntity> findVariableInstancesByScopeIdAndScopeTypeAndNames(String scopeId, String scopeType, Collection<String> variableNames) {
        Map<String, Object> params = new HashMap<>(3);
        params.put("scopeId", scopeId);
        params.put("scopeType", scopeType);
        params.put("variableNames", variableNames);
        return getList("selectVariableInstanceByScopeIdAndScopeTypeAndNames", params, variableInstanceByScopeIdAndScopeTypeAndVariableNamesMatcher, true);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstanceBySubScopeIdAndScopeType(String subScopeId, String scopeType) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("subScopeId", subScopeId);
        params.put("scopeType", scopeType);
        return getList("selectVariableInstancesBySubScopeIdAndScopeType", params, variableInstanceBySubScopeIdAndScopeTypeMatcher, true); 
    }

    @Override
    public VariableInstanceEntity findVariableInstanceBySubScopeIdAndScopeTypeAndName(String subScopeId, String scopeType, String variableName) {
        Map<String, String> params = new HashMap<>(3);
        params.put("subScopeId", subScopeId);
        params.put("scopeType", scopeType);
        params.put("variableName", variableName);
        return getEntity("selectVariableInstanceBySubScopeIdAndScopeTypeAndName", params, variableInstanceBySubScopeIdAndScopeTypeAndVariableNameMatcher, true);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesBySubScopeIdAndScopeTypeAndNames(String subScopeId, String scopeType, Collection<String> variableNames) {
        Map<String, Object> params = new HashMap<>(3);
        params.put("subScopeId", subScopeId);
        params.put("scopeType", scopeType);
        params.put("variableNames", variableNames);
        return getList("selectVariableInstanceBySubScopeIdAndScopeTypeAndNames", params, variableInstanceBySubScopeIdAndScopeTypeAndVariableNamesMatcher, true);
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

}
