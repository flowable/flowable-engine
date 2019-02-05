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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.data.HistoricTaskInstanceDataManager;
import org.flowable.task.service.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 */
public class MybatisHistoricTaskInstanceDataManager extends AbstractDataManager<HistoricTaskInstanceEntity> implements HistoricTaskInstanceDataManager {

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
        return (Long) getDbSqlSession().selectOne("selectHistoricTaskInstanceCountByQueryCriteria", historicTaskInstanceQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricTaskInstance> findHistoricTaskInstancesByQueryCriteria(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
        return getDbSqlSession().selectList("selectHistoricTaskInstancesByQueryCriteria", historicTaskInstanceQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricTaskInstance> findHistoricTaskInstancesAndRelatedEntitiesByQueryCriteria(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
        // paging doesn't work for combining task instances and variables
        // due to an outer join, so doing it in-memory

        int firstResult = historicTaskInstanceQuery.getFirstResult();
        int maxResults = historicTaskInstanceQuery.getMaxResults();

        // setting max results, limit to 20000 results for performance reasons
        if (historicTaskInstanceQuery.getTaskVariablesLimit() != null) {
            historicTaskInstanceQuery.setMaxResults(historicTaskInstanceQuery.getTaskVariablesLimit());
        } else {
            historicTaskInstanceQuery.setMaxResults(CommandContextUtil.getTaskServiceConfiguration().getHistoricTaskQueryLimit());
        }
        historicTaskInstanceQuery.setFirstResult(0);

        List<HistoricTaskInstance> instanceList = getDbSqlSession().selectListWithRawParameterNoCacheCheck("selectHistoricTaskInstancesWithRelatedEntitiesByQueryCriteria", historicTaskInstanceQuery);

        if (instanceList != null && !instanceList.isEmpty()) {
            if (firstResult > 0) {
                if (firstResult <= instanceList.size()) {
                    int toIndex = firstResult + Math.min(maxResults, instanceList.size() - firstResult);
                    return instanceList.subList(firstResult, toIndex);
                } else {
                    return Collections.EMPTY_LIST;
                }
            } else {
                int toIndex = maxResults > 0 ?  Math.min(maxResults, instanceList.size()) : instanceList.size();
                return instanceList.subList(0, toIndex);
            }
        }

        return instanceList;
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

}
