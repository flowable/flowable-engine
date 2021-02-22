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
package org.flowable.eventregistry.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.impl.EventDefinitionQueryImpl;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntityImpl;
import org.flowable.eventregistry.impl.persistence.entity.data.AbstractEventDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.EventDefinitionDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisEventDefinitionDataManager extends AbstractEventDataManager<EventDefinitionEntity> implements EventDefinitionDataManager {

    public MybatisEventDefinitionDataManager(EventRegistryEngineConfiguration eventRegistryConfiguration) {
        super(eventRegistryConfiguration);
    }

    @Override
    public Class<? extends EventDefinitionEntity> getManagedEntityClass() {
        return EventDefinitionEntityImpl.class;
    }

    @Override
    public EventDefinitionEntity create() {
        return new EventDefinitionEntityImpl();
    }

    @Override
    public EventDefinitionEntity findLatestEventDefinitionByKey(String eventDefinitionKey) {
        return (EventDefinitionEntity) getDbSqlSession().selectOne("selectLatestEventDefinitionByKey", eventDefinitionKey);
    }

    @Override
    public EventDefinitionEntity findLatestEventDefinitionByKeyAndTenantId(String eventDefinitionKey, String tenantId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("eventDefinitionKey", eventDefinitionKey);
        params.put("tenantId", tenantId);
        return (EventDefinitionEntity) getDbSqlSession().selectOne("selectLatestEventDefinitionByKeyAndTenantId", params);
    }

    @Override
    public EventDefinitionEntity findLatestEventDefinitionByKeyAndParentDeploymentId(String eventDefinitionKey, String parentDeploymentId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("eventDefinitionKey", eventDefinitionKey);
        params.put("parentDeploymentId", parentDeploymentId);
        return (EventDefinitionEntity) getDbSqlSession().selectOne("selectEventDefinitionByKeyAndParentDeploymentId", params);
    }

    @Override
    public EventDefinitionEntity findLatestEventDefinitionByKeyParentDeploymentIdAndTenantId(String eventDefinitionKey, String parentDeploymentId, String tenantId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("eventDefinitionKey", eventDefinitionKey);
        params.put("parentDeploymentId", parentDeploymentId);
        params.put("tenantId", tenantId);
        return (EventDefinitionEntity) getDbSqlSession().selectOne("selectEventDefinitionByKeyParentDeploymentIdAndTenantId", params);
    }

    @Override
    public void deleteEventDefinitionsByDeploymentId(String deploymentId) {
        getDbSqlSession().delete("deleteEventDefinitionsByDeploymentId", deploymentId, getManagedEntityClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<EventDefinition> findEventDefinitionsByQueryCriteria(EventDefinitionQueryImpl eventDefinitionQuery) {
        return getDbSqlSession().selectList("selectEventDefinitionsByQueryCriteria", eventDefinitionQuery);
    }

    @Override
    public long findEventDefinitionCountByQueryCriteria(EventDefinitionQueryImpl eventDefinitionQuery) {
        return (Long) getDbSqlSession().selectOne("selectEventDefinitionCountByQueryCriteria", eventDefinitionQuery);
    }

    @Override
    public EventDefinitionEntity findEventDefinitionByDeploymentAndKey(String deploymentId, String eventDefinitionKey) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deploymentId", deploymentId);
        parameters.put("eventDefinitionKey", eventDefinitionKey);
        return (EventDefinitionEntity) getDbSqlSession().selectOne("selectEventDefinitionByDeploymentAndKey", parameters);
    }

    @Override
    public EventDefinitionEntity findEventDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String eventDefinitionKey, String tenantId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deploymentId", deploymentId);
        parameters.put("eventDefinitionKey", eventDefinitionKey);
        parameters.put("tenantId", tenantId);
        return (EventDefinitionEntity) getDbSqlSession().selectOne("selectEventDefinitionByDeploymentAndKeyAndTenantId", parameters);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public EventDefinitionEntity findEventDefinitionByKeyAndVersion(String eventDefinitionKey, Integer eventVersion) {
        Map<String, Object> params = new HashMap<>();
        params.put("eventDefinitionKey", eventDefinitionKey);
        params.put("eventVersion", eventVersion);
        List<EventDefinitionEntity> results = getDbSqlSession().selectList("selectEventDefinitionsByKeyAndVersion", params);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("There are " + results.size() + " event definitions with key = '" + eventDefinitionKey + "' and version = '" + eventVersion + "'.");
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventDefinitionEntity findEventDefinitionByKeyAndVersionAndTenantId(String eventDefinitionKey, Integer eventVersion, String tenantId) {
        Map<String, Object> params = new HashMap<>();
        params.put("eventDefinitionKey", eventDefinitionKey);
        params.put("eventVersion", eventVersion);
        params.put("tenantId", tenantId);
        List<EventDefinitionEntity> results = getDbSqlSession().selectList("selectEventDefinitionsByKeyAndVersionAndTenantId", params);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("There are " + results.size() + " event definitions with key = '" + eventDefinitionKey + "' and version = '" + eventVersion + "'.");
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<EventDefinition> findEventDefinitionsByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectEventDefinitionByNativeQuery", parameterMap);
    }

    @Override
    public long findEventDefinitionCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectEventDefinitionCountByNativeQuery", parameterMap);
    }

    @Override
    public void updateEventDefinitionTenantIdForDeployment(String deploymentId, String newTenantId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().update("updateEventDefinitionTenantIdForDeploymentId", params);
    }

}
