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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.service.impl.HistoricVariableInstanceQueryImpl;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.data.HistoricVariableInstanceDataManager;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.HistoricVariableInstanceByProcInstMatcher;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.HistoricVariableInstanceByScopeIdAndScopeTypeMatcher;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.HistoricVariableInstanceBySubScopeIdAndScopeTypeMatcher;
import org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher.HistoricVariableInstanceByTaskIdMatcher;

/**
 * @author Joram Barrez
 */
public class MybatisHistoricVariableInstanceDataManager extends AbstractDataManager<HistoricVariableInstanceEntity> implements HistoricVariableInstanceDataManager {

    protected CachedEntityMatcher<HistoricVariableInstanceEntity> historicVariableInstanceByTaskIdMatcher 
        = new HistoricVariableInstanceByTaskIdMatcher();

    protected CachedEntityMatcher<HistoricVariableInstanceEntity> historicVariableInstanceByProcInstMatcher 
        = new HistoricVariableInstanceByProcInstMatcher();
    
    protected CachedEntityMatcher<HistoricVariableInstanceEntity> historicVariableInstanceByScopeIdAndScopeTypeMatcher 
        = new HistoricVariableInstanceByScopeIdAndScopeTypeMatcher();
    
    protected CachedEntityMatcher<HistoricVariableInstanceEntity> historicVariableInstanceBySubScopeIdAndScopeTypeMatcher 
        = new HistoricVariableInstanceBySubScopeIdAndScopeTypeMatcher();
    
    @Override
    public Class<? extends HistoricVariableInstanceEntity> getManagedEntityClass() {
        return HistoricVariableInstanceEntityImpl.class;
    }

    @Override
    public HistoricVariableInstanceEntity create() {
        return new HistoricVariableInstanceEntityImpl();
    }

    @Override
    public void insert(HistoricVariableInstanceEntity entity) {
        super.insert(entity);
    }

    @Override
    public List<HistoricVariableInstanceEntity> findHistoricVariableInstancesByProcessInstanceId(final String processInstanceId) {
        return getList("selectHistoricVariableInstanceByProcessInstanceId", processInstanceId, historicVariableInstanceByProcInstMatcher, true);
    }

    @Override
    public List<HistoricVariableInstanceEntity> findHistoricVariableInstancesByTaskId(final String taskId) {
        return getList("selectHistoricVariableInstanceByTaskId", taskId, historicVariableInstanceByTaskIdMatcher, true);
    }

    @Override
    public long findHistoricVariableInstanceCountByQueryCriteria(HistoricVariableInstanceQueryImpl historicProcessVariableQuery) {
        return (Long) getDbSqlSession().selectOne("selectHistoricVariableInstanceCountByQueryCriteria", historicProcessVariableQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricVariableInstance> findHistoricVariableInstancesByQueryCriteria(HistoricVariableInstanceQueryImpl historicProcessVariableQuery) {
        return getDbSqlSession().selectList("selectHistoricVariableInstanceByQueryCriteria", historicProcessVariableQuery);
    }

    @Override
    public HistoricVariableInstanceEntity findHistoricVariableInstanceByVariableInstanceId(String variableInstanceId) {
        return (HistoricVariableInstanceEntity) getDbSqlSession().selectOne("selectHistoricVariableInstanceByVariableInstanceId", variableInstanceId);
    }
    
    @Override
    public List<HistoricVariableInstanceEntity> findHistoricalVariableInstancesByScopeIdAndScopeType(String scopeId, String scopeType) {
        Map<String, String> params = new HashMap<>(2);
        params.put("scopeId", scopeId);
        params.put("scopeType", scopeType);
        return getList("selectHistoricVariableInstanceByScopeIdAndScopeType", params, historicVariableInstanceByScopeIdAndScopeTypeMatcher, true);
    }
    
    @Override
    public List<HistoricVariableInstanceEntity> findHistoricalVariableInstancesBySubScopeIdAndScopeType(String subScopeId, String scopeType) {
        Map<String, String> params = new HashMap<>(2);
        params.put("subScopeId", subScopeId);
        params.put("scopeType", scopeType);
        return getList("selectHistoricVariableInstanceByScopeIdAndScopeType", params, historicVariableInstanceByScopeIdAndScopeTypeMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricVariableInstance> findHistoricVariableInstancesByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectHistoricVariableInstanceByNativeQuery", parameterMap);
    }

    @Override
    public long findHistoricVariableInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectHistoricVariableInstanceCountByNativeQuery", parameterMap);
    }

}
