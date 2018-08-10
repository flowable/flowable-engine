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

import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.service.impl.HistoricVariableInstanceQueryImpl;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.data.HistoricVariableInstanceDataManager;

/**
 * @author Tijs Rademakers
 */
public class MongoDbHistoricVariableInstanceDataManager extends AbstractMongoDbDataManager implements HistoricVariableInstanceDataManager {
    
    public static final String COLLECTION_HISTORIC_VARIABLE_INSTANCES = "historicVariableInstances";

    @Override
    public HistoricVariableInstanceEntity create() {
        return new HistoricVariableInstanceEntityImpl();
    }

    @Override
    public HistoricVariableInstanceEntity findById(String entityId) {
        return getMongoDbSession().findOne(COLLECTION_HISTORIC_VARIABLE_INSTANCES, entityId);
    }

    @Override
    public void insert(HistoricVariableInstanceEntity entity) {
        getMongoDbSession().insertOne(entity);
    }

    @Override
    public HistoricVariableInstanceEntity update(HistoricVariableInstanceEntity entity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(String id) {
        HistoricVariableInstanceEntity variableEntity = findById(id);
        delete(variableEntity);
    }

    @Override
    public void delete(HistoricVariableInstanceEntity variableEntity) {
        getMongoDbSession().delete(COLLECTION_HISTORIC_VARIABLE_INSTANCES, variableEntity);
    }

    @Override
    public List<HistoricVariableInstanceEntity> findHistoricVariableInstancesByProcessInstanceId(String processInstanceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HistoricVariableInstanceEntity> findHistoricVariableInstancesByTaskId(String taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long findHistoricVariableInstanceCountByQueryCriteria(
            HistoricVariableInstanceQueryImpl historicProcessVariableQuery) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<HistoricVariableInstance> findHistoricVariableInstancesByQueryCriteria(
            HistoricVariableInstanceQueryImpl historicProcessVariableQuery) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HistoricVariableInstanceEntity findHistoricVariableInstanceByVariableInstanceId(String variableInstanceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HistoricVariableInstanceEntity> findHistoricalVariableInstancesByScopeIdAndScopeType(String scopeId,
            String scopeType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HistoricVariableInstanceEntity> findHistoricalVariableInstancesBySubScopeIdAndScopeType(
            String subScopeId, String scopeType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HistoricVariableInstance> findHistoricVariableInstancesByNativeQuery(Map<String, Object> parameterMap) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long findHistoricVariableInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        // TODO Auto-generated method stub
        return 0;
    }

}
