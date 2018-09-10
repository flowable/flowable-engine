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
public class MongoDbHistoricProcessInstanceDataManager extends AbstractMongoDbDataManager<HistoricProcessInstanceEntity> implements HistoricProcessInstanceDataManager {
    
    public static final String COLLECTION_HISTORIC_PROCESS_INSTANCES = "historicProcessInstances";

    @Override
    public String getCollection() {
        return COLLECTION_HISTORIC_PROCESS_INSTANCES;
    }
    
    @Override
    public HistoricProcessInstanceEntity create() {
        return new HistoricProcessInstanceEntityImpl();
    }

    @Override
    public BasicDBObject createUpdateObject(Entity entity) {
        HistoricProcessInstanceEntity instanceEntity = (HistoricProcessInstanceEntity) entity;
        BasicDBObject updateObject = null;
        updateObject = setUpdateProperty(instanceEntity, "deleteReason", instanceEntity.getDeleteReason(), updateObject);
        updateObject = setUpdateProperty(instanceEntity, "endActivityId", instanceEntity.getEndActivityId(), updateObject);
        updateObject = setUpdateProperty(instanceEntity, "endTime", instanceEntity.getEndTime(), updateObject);
        updateObject = setUpdateProperty(instanceEntity, "startActivityId", instanceEntity.getStartActivityId(), updateObject);
        updateObject = setUpdateProperty(instanceEntity, "startTime", instanceEntity.getStartTime(), updateObject);
        updateObject = setUpdateProperty(instanceEntity, "startUserId", instanceEntity.getStartUserId(), updateObject);
        return updateObject;
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
