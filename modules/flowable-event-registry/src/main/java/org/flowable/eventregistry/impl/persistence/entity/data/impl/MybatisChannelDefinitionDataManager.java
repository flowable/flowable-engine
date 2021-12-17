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
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.impl.ChannelDefinitionQueryImpl;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntityImpl;
import org.flowable.eventregistry.impl.persistence.entity.data.AbstractEventDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.ChannelDefinitionDataManager;

public class MybatisChannelDefinitionDataManager extends AbstractEventDataManager<ChannelDefinitionEntity> implements ChannelDefinitionDataManager {

    public MybatisChannelDefinitionDataManager(EventRegistryEngineConfiguration eventRegistryConfiguration) {
        super(eventRegistryConfiguration);
    }

    @Override
    public Class<? extends ChannelDefinitionEntity> getManagedEntityClass() {
        return ChannelDefinitionEntityImpl.class;
    }

    @Override
    public ChannelDefinitionEntity create() {
        return new ChannelDefinitionEntityImpl();
    }

    @Override
    public ChannelDefinitionEntity findLatestChannelDefinitionByKey(String channelDefinitionKey) {
        return (ChannelDefinitionEntity) getDbSqlSession().selectOne("selectLatestChannelDefinitionByKey", channelDefinitionKey);
    }

    @Override
    public ChannelDefinitionEntity findLatestChannelDefinitionByKeyAndTenantId(String channelDefinitionKey, String tenantId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("channelDefinitionKey", channelDefinitionKey);
        params.put("tenantId", tenantId);
        return (ChannelDefinitionEntity) getDbSqlSession().selectOne("selectLatestChannelDefinitionByKeyAndTenantId", params);
    }

    @Override
    public ChannelDefinitionEntity findLatestChannelDefinitionByKeyAndParentDeploymentId(String channelDefinitionKey, String parentDeploymentId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("channelDefinitionKey", channelDefinitionKey);
        params.put("parentDeploymentId", parentDeploymentId);
        return (ChannelDefinitionEntity) getDbSqlSession().selectOne("selectChannelDefinitionByKeyAndParentDeploymentId", params);
    }

    @Override
    public ChannelDefinitionEntity findLatestChannelDefinitionByKeyParentDeploymentIdAndTenantId(String channelDefinitionKey, String parentDeploymentId, String tenantId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("channelDefinitionKey", channelDefinitionKey);
        params.put("parentDeploymentId", parentDeploymentId);
        params.put("tenantId", tenantId);
        return (ChannelDefinitionEntity) getDbSqlSession().selectOne("selectChannelDefinitionByKeyParentDeploymentIdAndTenantId", params);
    }

    @Override
    public void deleteChannelDefinitionsByDeploymentId(String deploymentId) {
        getDbSqlSession().delete("deleteChannelDefinitionsByDeploymentId", deploymentId, getManagedEntityClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ChannelDefinition> findChannelDefinitionsByQueryCriteria(ChannelDefinitionQueryImpl ChannelDefinitionQuery) {
        return getDbSqlSession().selectList("selectChannelDefinitionsByQueryCriteria", ChannelDefinitionQuery);
    }

    @Override
    public long findChannelDefinitionCountByQueryCriteria(ChannelDefinitionQueryImpl ChannelDefinitionQuery) {
        return (Long) getDbSqlSession().selectOne("selectChannelDefinitionCountByQueryCriteria", ChannelDefinitionQuery);
    }

    @Override
    public ChannelDefinitionEntity findChannelDefinitionByDeploymentAndKey(String deploymentId, String channelDefinitionKey) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deploymentId", deploymentId);
        parameters.put("channelDefinitionKey", channelDefinitionKey);
        return (ChannelDefinitionEntity) getDbSqlSession().selectOne("selectChannelDefinitionByDeploymentAndKey", parameters);
    }

    @Override
    public ChannelDefinitionEntity findChannelDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String channelDefinitionKey, String tenantId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deploymentId", deploymentId);
        parameters.put("channelDefinitionKey", channelDefinitionKey);
        parameters.put("tenantId", tenantId);
        return (ChannelDefinitionEntity) getDbSqlSession().selectOne("selectChannelDefinitionByDeploymentAndKeyAndTenantId", parameters);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public ChannelDefinitionEntity findChannelDefinitionByKeyAndVersion(String channelDefinitionKey, Integer eventVersion) {
        Map<String, Object> params = new HashMap<>();
        params.put("channelDefinitionKey", channelDefinitionKey);
        params.put("eventVersion", eventVersion);
        List<ChannelDefinitionEntity> results = getDbSqlSession().selectList("selectChannelDefinitionsByKeyAndVersion", params);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("There are " + results.size() + " event definitions with key = '" + channelDefinitionKey + "' and version = '" + eventVersion + "'.");
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChannelDefinitionEntity findChannelDefinitionByKeyAndVersionAndTenantId(String channelDefinitionKey, Integer eventVersion, String tenantId) {
        Map<String, Object> params = new HashMap<>();
        params.put("channelDefinitionKey", channelDefinitionKey);
        params.put("eventVersion", eventVersion);
        params.put("tenantId", tenantId);
        List<ChannelDefinitionEntity> results = getDbSqlSession().selectList("selectChannelDefinitionsByKeyAndVersionAndTenantId", params);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("There are " + results.size() + " event definitions with key = '" + channelDefinitionKey + "' and version = '" + eventVersion + "'.");
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ChannelDefinition> findChannelDefinitionsByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectChannelDefinitionByNativeQuery", parameterMap);
    }

    @Override
    public long findChannelDefinitionCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectChannelDefinitionCountByNativeQuery", parameterMap);
    }

    @Override
    public void updateChannelDefinitionTenantIdForDeployment(String deploymentId, String newTenantId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().update("updateChannelDefinitionTenantIdForDeploymentId", params);
    }

    @Override
    public void updateChannelDefinitionTypeAndImplementation(String channelDefinitionId, String type, String implementation) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", channelDefinitionId);
        params.put("type", type);
        params.put("implementation", implementation);
        getDbSqlSession().update("updateChannelDefinitionTypeAndImplementationById", params);
    }
}
