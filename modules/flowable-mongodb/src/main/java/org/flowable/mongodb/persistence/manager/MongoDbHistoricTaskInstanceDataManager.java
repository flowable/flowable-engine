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
package org.flowable.mongodb.persistence.manager;

import java.util.List;
import java.util.Map;

import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.data.HistoricTaskInstanceDataManager;

/**
 * @author Tijs Rademakers
 */
public class MongoDbHistoricTaskInstanceDataManager extends AbstractMongoDbDataManager implements HistoricTaskInstanceDataManager {
    
    public static final String COLLECTION_HISTORIC_TASK_INSTANCES = "historicTaskInstances";

    @Override
    public HistoricTaskInstanceEntity create() {
        return new HistoricTaskInstanceEntityImpl();
    }

    @Override
    public HistoricTaskInstanceEntity findById(String taskId) {
        return getMongoDbSession().findOne(COLLECTION_HISTORIC_TASK_INSTANCES, taskId);
    }

    @Override
    public void insert(HistoricTaskInstanceEntity entity) {
        getMongoDbSession().insertOne(entity);
    }

    @Override
    public HistoricTaskInstanceEntity update(HistoricTaskInstanceEntity entity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(String id) {
        HistoricTaskInstanceEntity taskEntity = findById(id);
        delete(taskEntity);
    }

    @Override
    public void delete(HistoricTaskInstanceEntity taskEntity) {
        getMongoDbSession().delete(COLLECTION_HISTORIC_TASK_INSTANCES, taskEntity);
    }

    @Override
    public HistoricTaskInstanceEntity create(TaskEntity task) {
        return new HistoricTaskInstanceEntityImpl(task);
    }

    @Override
    public List<HistoricTaskInstanceEntity> findHistoricTasksByParentTaskId(String parentTaskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HistoricTaskInstanceEntity> findHistoricTasksByProcessInstanceId(String processInstanceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long findHistoricTaskInstanceCountByQueryCriteria(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<HistoricTaskInstance> findHistoricTaskInstancesByQueryCriteria(
            HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HistoricTaskInstance> findHistoricTaskInstancesAndRelatedEntitiesByQueryCriteria(
            HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HistoricTaskInstance> findHistoricTaskInstancesByNativeQuery(Map<String, Object> parameterMap) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long findHistoricTaskInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        // TODO Auto-generated method stub
        return 0;
    }
    
}
