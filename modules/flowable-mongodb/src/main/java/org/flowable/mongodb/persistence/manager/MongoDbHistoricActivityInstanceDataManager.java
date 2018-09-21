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
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.HistoricActivityInstanceQueryImpl;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.HistoricActivityInstanceDataManager;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class MongoDbHistoricActivityInstanceDataManager extends AbstractMongoDbDataManager<HistoricActivityInstanceEntity> implements HistoricActivityInstanceDataManager {
    
    public static final String COLLECTION_HISTORIC_ACTIVITY_INSTANCES = "historicActivityInstances";

    @Override
    public String getCollection() {
        return COLLECTION_HISTORIC_ACTIVITY_INSTANCES;
    }
    
    @Override
    public HistoricActivityInstanceEntity create() {
        return new HistoricActivityInstanceEntityImpl();
    }
    
    @Override
    public BasicDBObject createUpdateObject(Entity entity) {
        return null;
    }

    @Override
    public List<HistoricActivityInstanceEntity> findUnfinishedHistoricActivityInstancesByExecutionAndActivityId(String executionId, String activityId) {
        Bson filter = Filters.and(Filters.eq("executionId", executionId), Filters.eq("activityId", activityId));
        return getMongoDbSession().find(COLLECTION_HISTORIC_ACTIVITY_INSTANCES, filter);
    }

    @Override
    public List<HistoricActivityInstanceEntity> findHistoricActivityInstancesByExecutionIdAndActivityId(String executionId, String activityId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<HistoricActivityInstanceEntity> findUnfinishedHistoricActivityInstancesByProcessInstanceId(String processInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteHistoricActivityInstancesByProcessInstanceId(String historicProcessInstanceId) {
        getMongoDbSession().getCollection(COLLECTION_HISTORIC_ACTIVITY_INSTANCES).deleteMany(Filters.eq("processInstanceId", historicProcessInstanceId));
    }

    @Override
    public long findHistoricActivityInstanceCountByQueryCriteria(HistoricActivityInstanceQueryImpl historicActivityInstanceQuery) {
        return getMongoDbSession().count(COLLECTION_HISTORIC_ACTIVITY_INSTANCES, createFilter(historicActivityInstanceQuery));
    }

    @Override
    public List<HistoricActivityInstance> findHistoricActivityInstancesByQueryCriteria(HistoricActivityInstanceQueryImpl historicActivityInstanceQuery) {
        return getMongoDbSession().find(COLLECTION_HISTORIC_ACTIVITY_INSTANCES, createFilter(historicActivityInstanceQuery));
    }

    @Override
    public List<HistoricActivityInstance> findHistoricActivityInstancesByNativeQuery(Map<String, Object> parameterMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long findHistoricActivityInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
        throw new UnsupportedOperationException();
    }
    
    protected Bson createFilter(HistoricActivityInstanceQueryImpl activityInstanceQuery) {
        List<Bson> filters = new ArrayList<>();
        if (activityInstanceQuery.getExecutionId() != null) {
            filters.add(Filters.eq("executionId", activityInstanceQuery.getExecutionId()));
        }
        
        if (activityInstanceQuery.getProcessInstanceId() != null) {
            filters.add(Filters.eq("processInstanceId", activityInstanceQuery.getProcessInstanceId()));
        }
        
        if (activityInstanceQuery.getActivityId() != null) {
            filters.add(Filters.eq("activityId", activityInstanceQuery.getActivityId()));
        }
        
        if (activityInstanceQuery.getActivityName() != null) {
            filters.add(Filters.eq("activityName", activityInstanceQuery.getActivityName()));
        }
        if (activityInstanceQuery.getProcessDefinitionId() != null) {
            filters.add(Filters.eq("processDefinitionId", activityInstanceQuery.getProcessDefinitionId()));
        }
        if (activityInstanceQuery.getActivityType() != null) {
            filters.add(Filters.eq("activityType", activityInstanceQuery.getActivityType()));
        }
        if (activityInstanceQuery.getAssignee() != null) {
            filters.add(Filters.eq("assignee", activityInstanceQuery.getAssignee()));
        }
        if (activityInstanceQuery.isFinished()) {
            filters.add(Filters.not(Filters.exists("endTime")));
        }
        if (activityInstanceQuery.isUnfinished()) {
            filters.add(Filters.exists("endTime"));
        }
        if (activityInstanceQuery.getDeleteReason() != null) {
            filters.add(Filters.eq("deleteReason", activityInstanceQuery.getDeleteReason()));
        }
        if (activityInstanceQuery.getDeleteReasonLike() != null) {
            filters.add(Filters.regex("deleteReason", activityInstanceQuery.getDeleteReasonLike().replace("%", ".*")));
        }
        if (activityInstanceQuery.getTenantId() != null) {
            filters.add(Filters.eq("tenantId", activityInstanceQuery.getTenantId()));
        }
        if (activityInstanceQuery.getTenantIdLike() != null) {
            filters.add(Filters.regex("tenantId", activityInstanceQuery.getTenantIdLike().replace("%", ".*")));
        }
        if (activityInstanceQuery.isWithoutTenantId()) {
            filters.add(Filters.or(Filters.eq("tenantId", ProcessEngineConfiguration.NO_TENANT_ID), Filters.not(Filters.exists("tenantId"))));
        }
        return makeAndFilter(filters);
    }
}
