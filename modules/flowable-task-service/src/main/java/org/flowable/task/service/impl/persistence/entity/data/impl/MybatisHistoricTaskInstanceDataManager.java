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
package org.flowable.task.service.impl.persistence.entity.data.impl;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.data.HistoricTaskInstanceDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisHistoricTaskInstanceDataManager extends AbstractDataManager<HistoricTaskInstanceEntity> implements HistoricTaskInstanceDataManager {

    protected TaskServiceConfiguration taskServiceConfiguration;
    
    public MybatisHistoricTaskInstanceDataManager(TaskServiceConfiguration taskServiceConfiguration) {
        this.taskServiceConfiguration = taskServiceConfiguration;
    }
    
    @Override
    public Class<? extends HistoricTaskInstanceEntity> getManagedEntityClass() {
        return HistoricTaskInstanceEntityImpl.class;
    }

    @Override
    public HistoricTaskInstanceEntity create() {
        return new HistoricTaskInstanceEntityImpl();
    }

    @Override
    public HistoricTaskInstanceEntity create(TaskEntity task) {
        return new HistoricTaskInstanceEntityImpl(task);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<HistoricTaskInstanceEntity> findHistoricTasksByParentTaskId(String parentTaskId) {
        return getDbSqlSession().selectList("selectHistoricTasksByParentTaskId", parentTaskId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricTaskInstanceEntity> findHistoricTasksByProcessInstanceId(String processInstanceId) {
        return getDbSqlSession().selectList("selectHistoricTaskInstancesByProcessInstanceId", processInstanceId);
    }

    @Override
    public long findHistoricTaskInstanceCountByQueryCriteria(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
        setSafeInValueLists(historicTaskInstanceQuery);
        return (Long) getDbSqlSession().selectOne("selectHistoricTaskInstanceCountByQueryCriteria", historicTaskInstanceQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricTaskInstance> findHistoricTaskInstancesByQueryCriteria(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
        setSafeInValueLists(historicTaskInstanceQuery);
        return getDbSqlSession().selectList("selectHistoricTaskInstancesByQueryCriteria", historicTaskInstanceQuery, getManagedEntityClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricTaskInstance> findHistoricTaskInstancesAndRelatedEntitiesByQueryCriteria(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
        setSafeInValueLists(historicTaskInstanceQuery);
        return getDbSqlSession().selectList("selectHistoricTaskInstancesWithRelatedEntitiesByQueryCriteria", historicTaskInstanceQuery, getManagedEntityClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricTaskInstance> findHistoricTaskInstancesByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectHistoricTaskInstanceByNativeQuery", parameterMap);
    }

    @Override
    public long findHistoricTaskInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectHistoricTaskInstanceCountByNativeQuery", parameterMap);
    }
    
    @Override
    public void deleteHistoricTaskInstances(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
        getDbSqlSession().delete("bulkDeleteHistoricTaskInstances", historicTaskInstanceQuery, HistoricTaskInstanceEntityImpl.class);
    }

    @Override
    public void deleteHistoricTaskInstancesForNonExistingProcessInstances() {
        getDbSqlSession().delete("bulkDeleteHistoricTaskInstancesForNonExistingProcessInstances", null, HistoricTaskInstanceEntityImpl.class);
    }
    
    @Override
    public void deleteHistoricTaskInstancesForNonExistingCaseInstances() {
        getDbSqlSession().delete("bulkDeleteHistoricTaskInstancesForNonExistingCaseInstances", null, HistoricTaskInstanceEntityImpl.class);
    }

    @Override
    protected IdGenerator getIdGenerator() {
        return taskServiceConfiguration.getIdGenerator();
    }
    
    protected void setSafeInValueLists(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
        if (historicTaskInstanceQuery.getCandidateGroups() != null) {
            historicTaskInstanceQuery.setSafeCandidateGroups(createSafeInValuesList(historicTaskInstanceQuery.getCandidateGroups()));
        }
        
        if (historicTaskInstanceQuery.getInvolvedGroups() != null) {
            historicTaskInstanceQuery.setSafeInvolvedGroups(createSafeInValuesList(historicTaskInstanceQuery.getInvolvedGroups()));
        }
        
        if (historicTaskInstanceQuery.getOrQueryObjects() != null && !historicTaskInstanceQuery.getOrQueryObjects().isEmpty()) {
            for (HistoricTaskInstanceQueryImpl orHistoricTaskInstanceQuery : historicTaskInstanceQuery.getOrQueryObjects()) {
                setSafeInValueLists(orHistoricTaskInstanceQuery);
            }
        }
    }
}
