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
package org.flowable.engine.impl.persistence.entity.data.impl;

import java.util.List;
import java.util.Map;

import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.HistoricProcessInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.AbstractProcessDataManager;
import org.flowable.engine.impl.persistence.entity.data.HistoricProcessInstanceDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisHistoricProcessInstanceDataManager extends AbstractProcessDataManager<HistoricProcessInstanceEntity> implements HistoricProcessInstanceDataManager {

    public MybatisHistoricProcessInstanceDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public Class<? extends HistoricProcessInstanceEntity> getManagedEntityClass() {
        return HistoricProcessInstanceEntityImpl.class;
    }

    @Override
    public HistoricProcessInstanceEntity create() {
        return new HistoricProcessInstanceEntityImpl();
    }

    @Override
    public HistoricProcessInstanceEntity create(ExecutionEntity processInstanceExecutionEntity) {
        return new HistoricProcessInstanceEntityImpl(processInstanceExecutionEntity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> findHistoricProcessInstanceIdsByProcessDefinitionId(String processDefinitionId) {
        return getDbSqlSession().selectList("selectHistoricProcessInstanceIdsByProcessDefinitionId", processDefinitionId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricProcessInstance> findHistoricProcessInstancesBySuperProcessInstanceId(String superProcessInstanceId) {
        return getDbSqlSession().selectList("selectHistoricProcessInstanceIdsBySuperProcessInstanceId", superProcessInstanceId);
    }

    @Override
    public long findHistoricProcessInstanceCountByQueryCriteria(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        setSafeInValueLists(historicProcessInstanceQuery);
        return (Long) getDbSqlSession().selectOne("selectHistoricProcessInstanceCountByQueryCriteria", historicProcessInstanceQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricProcessInstance> findHistoricProcessInstancesByQueryCriteria(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        setSafeInValueLists(historicProcessInstanceQuery);
        return getDbSqlSession().selectList("selectHistoricProcessInstancesByQueryCriteria", historicProcessInstanceQuery, getManagedEntityClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricProcessInstance> findHistoricProcessInstancesAndVariablesByQueryCriteria(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        setSafeInValueLists(historicProcessInstanceQuery);
        return getDbSqlSession().selectList("selectHistoricProcessInstancesWithVariablesByQueryCriteria", historicProcessInstanceQuery, getManagedEntityClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricProcessInstance> findHistoricProcessInstancesByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectHistoricProcessInstanceByNativeQuery", parameterMap);
    }

    @Override
    public long findHistoricProcessInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectHistoricProcessInstanceCountByNativeQuery", parameterMap);
    }

    @Override
    public void deleteHistoricProcessInstances(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        getDbSqlSession().delete("bulkDeleteHistoricProcessInstances", historicProcessInstanceQuery, HistoricProcessInstanceEntityImpl.class);
    }

    protected void setSafeInValueLists(HistoricProcessInstanceQueryImpl processInstanceQuery) {
        if (processInstanceQuery.getInvolvedGroups() != null) {
            processInstanceQuery.setSafeInvolvedGroups(createSafeInValuesList(processInstanceQuery.getInvolvedGroups()));
        }
        
        if (processInstanceQuery.getOrQueryObjects() != null && !processInstanceQuery.getOrQueryObjects().isEmpty()) {
            for (HistoricProcessInstanceQueryImpl orProcessInstanceQuery : processInstanceQuery.getOrQueryObjects()) {
                setSafeInValueLists(orProcessInstanceQuery);
            }
        }
    }
}
