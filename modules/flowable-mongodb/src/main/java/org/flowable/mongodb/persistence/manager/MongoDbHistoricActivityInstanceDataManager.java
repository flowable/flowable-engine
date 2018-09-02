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
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.HistoricActivityInstanceQueryImpl;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.HistoricActivityInstanceDataManager;

import com.mongodb.client.model.Filters;

/**
 * @author Tijs Rademakers
 */
public class MongoDbHistoricActivityInstanceDataManager extends AbstractMongoDbDataManager implements HistoricActivityInstanceDataManager {
    
    public static final String COLLECTION_HISTORIC_ACTIVITY_INSTANCES = "historicActivityInstances";

    @Override
    public HistoricActivityInstanceEntity create() {
        return new HistoricActivityInstanceEntityImpl();
    }

    @Override
    public HistoricActivityInstanceEntity findById(String instanceId) {
        return getMongoDbSession().findOne(COLLECTION_HISTORIC_ACTIVITY_INSTANCES, instanceId);
    }

    @Override
    public void insert(HistoricActivityInstanceEntity entity) {
        getMongoDbSession().insertOne(entity);
    }

    @Override
    public HistoricActivityInstanceEntity update(HistoricActivityInstanceEntity entity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(String id) {
        HistoricActivityInstanceEntity instanceEntity = findById(id);
        delete(instanceEntity);
    }

    @Override
    public void delete(HistoricActivityInstanceEntity instanceEntity) {
        getMongoDbSession().delete(COLLECTION_HISTORIC_ACTIVITY_INSTANCES, instanceEntity);
    }

    @Override
    public List<HistoricActivityInstanceEntity> findUnfinishedHistoricActivityInstancesByExecutionAndActivityId(String executionId, String activityId) {
        Bson filter = Filters.and(Filters.eq("executionId", executionId), Filters.eq("activityId", activityId));
        return getMongoDbSession().find(COLLECTION_HISTORIC_ACTIVITY_INSTANCES, filter);
    }

    @Override
    public List<HistoricActivityInstanceEntity> findHistoricActivityInstancesByExecutionIdAndActivityId(
            String executionId, String activityId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HistoricActivityInstanceEntity> findUnfinishedHistoricActivityInstancesByProcessInstanceId(
            String processInstanceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteHistoricActivityInstancesByProcessInstanceId(String historicProcessInstanceId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public long findHistoricActivityInstanceCountByQueryCriteria(HistoricActivityInstanceQueryImpl historicActivityInstanceQuery) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<HistoricActivityInstance> findHistoricActivityInstancesByQueryCriteria(HistoricActivityInstanceQueryImpl historicActivityInstanceQuery) {
        return getMongoDbSession().find(COLLECTION_HISTORIC_ACTIVITY_INSTANCES, createFilter(historicActivityInstanceQuery));
    }

    @Override
    public List<HistoricActivityInstance> findHistoricActivityInstancesByNativeQuery(Map<String, Object> parameterMap) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long findHistoricActivityInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    protected Bson createFilter(HistoricActivityInstanceQueryImpl activityInstanceQuery) {
        List<Bson> andFilters = new ArrayList<>();
        if (activityInstanceQuery.getExecutionId() != null) {
            andFilters.add(Filters.eq("executionId", activityInstanceQuery.getExecutionId()));
        }
        
        if (activityInstanceQuery.getProcessInstanceId() != null) {
            andFilters.add(Filters.eq("processInstanceId", activityInstanceQuery.getProcessInstanceId()));
        }
        
        if (activityInstanceQuery.getActivityId() != null) {
            andFilters.add(Filters.eq("activityId", activityInstanceQuery.getActivityId()));
        }
        
        if (activityInstanceQuery.getActivityName() != null) {
            andFilters.add(Filters.eq("activityName", activityInstanceQuery.getActivityName()));
        }
        
        Bson filter = null;
        if (andFilters.size() > 0) {
            filter = Filters.and(andFilters.toArray(new Bson[andFilters.size()]));
        }
        
        return filter;
    }
}
