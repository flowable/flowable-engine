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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.HistoricProcessInstanceQueryImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.HistoricProcessInstanceDataManager;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;

/**
 * @author Tijs Rademakers
 */
public class MongoDbHistoricProcessInstanceDataManager extends AbstractMongoDbDataManager implements HistoricProcessInstanceDataManager {
    
    public static final String COLLECTION_HISTORIC_PROCESS_INSTANCES = "historicProcessInstances";

    @Override
    public HistoricProcessInstanceEntity create() {
        return new HistoricProcessInstanceEntityImpl();
    }

    @Override
    public HistoricProcessInstanceEntity findById(String instanceId) {
        return getMongoDbSession().findOne(COLLECTION_HISTORIC_PROCESS_INSTANCES, instanceId);
    }

    @Override
    public void insert(HistoricProcessInstanceEntity entity) {
        getMongoDbSession().insertOne(entity);
    }

    @Override
    public HistoricProcessInstanceEntity update(HistoricProcessInstanceEntity entity) {
        getMongoDbSession().update(entity);
        return entity;
    }
    
    @Override
    public void updateEntity(Entity entity) {
        HistoricProcessInstanceEntity instanceEntity = (HistoricProcessInstanceEntity) entity;
        Map<String, Object> persistentState = (Map<String, Object>) entity.getOriginalPersistentState();
        BasicDBObject updateObject = null;
        updateObject = setUpdateProperty("deleteReason", instanceEntity.getDeleteReason(), persistentState, updateObject);
        updateObject = setUpdateProperty("endActivityId", instanceEntity.getEndActivityId(), persistentState, updateObject);
        updateObject = setUpdateProperty("endTime", instanceEntity.getEndTime(), persistentState, updateObject);
        updateObject = setUpdateProperty("startActivityId", instanceEntity.getStartActivityId(), persistentState, updateObject);
        updateObject = setUpdateProperty("startTime", instanceEntity.getStartTime(), persistentState, updateObject);
        updateObject = setUpdateProperty("startUserId", instanceEntity.getStartUserId(), persistentState, updateObject);
        
        if (updateObject != null) {
            getMongoDbSession().performUpdate(COLLECTION_HISTORIC_PROCESS_INSTANCES, entity, new Document().append("$set", updateObject));
        }
    }

    @Override
    public void delete(String id) {
        HistoricProcessInstanceEntity instanceEntity = findById(id);
        delete(instanceEntity);
    }

    @Override
    public void delete(HistoricProcessInstanceEntity instanceEntity) {
        getMongoDbSession().delete(COLLECTION_HISTORIC_PROCESS_INSTANCES, instanceEntity);
    }

    @Override
    public HistoricProcessInstanceEntity create(ExecutionEntity processInstanceExecutionEntity) {
        return new HistoricProcessInstanceEntityImpl(processInstanceExecutionEntity);
    }

    @Override
    public List<String> findHistoricProcessInstanceIdsByProcessDefinitionId(String processDefinitionId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HistoricProcessInstance> findHistoricProcessInstancesBySuperProcessInstanceId(
            String superProcessInstanceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long findHistoricProcessInstanceCountByQueryCriteria(
            HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<HistoricProcessInstance> findHistoricProcessInstancesByQueryCriteria(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        return getMongoDbSession().find(COLLECTION_HISTORIC_PROCESS_INSTANCES, createFilter(historicProcessInstanceQuery));
    }

    @Override
    public List<HistoricProcessInstance> findHistoricProcessInstancesAndVariablesByQueryCriteria(
            HistoricProcessInstanceQueryImpl historicProcessInstanceQuery) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HistoricProcessInstance> findHistoricProcessInstancesByNativeQuery(Map<String, Object> parameterMap) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long findHistoricProcessInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    protected Bson createFilter(HistoricProcessInstanceQueryImpl processInstanceQuery) {
        List<Bson> andFilters = new ArrayList<>();
        if (processInstanceQuery.getProcessInstanceId() != null) {
            andFilters.add(Filters.eq("processInstanceId", processInstanceQuery.getProcessInstanceId()));
        }
        
        if (processInstanceQuery.getDeploymentId() != null) {
            andFilters.add(Filters.eq("deploymentId", processInstanceQuery.getDeploymentId()));
        }
        
        if (processInstanceQuery.getProcessDefinitionId() != null) {
            andFilters.add(Filters.eq("processDefinitionId", processInstanceQuery.getProcessDefinitionId()));
        }
        
        if (processInstanceQuery.getSuperProcessInstanceId() != null) {
            andFilters.add(Filters.eq("superProcessInstanceId", processInstanceQuery.getSuperProcessInstanceId()));
        }
        
        Bson filter = null;
        if (andFilters.size() > 0) {
            filter = Filters.and(andFilters.toArray(new Bson[andFilters.size()]));
        }
        
        return filter;
    }
    
}
