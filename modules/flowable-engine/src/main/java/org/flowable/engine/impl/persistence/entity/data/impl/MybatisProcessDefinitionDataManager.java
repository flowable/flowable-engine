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
package org.flowable.engine.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.AbstractProcessDataManager;
import org.flowable.engine.impl.persistence.entity.data.ProcessDefinitionDataManager;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * @author Joram Barrez
 */
public class MybatisProcessDefinitionDataManager extends AbstractProcessDataManager<ProcessDefinitionEntity> implements ProcessDefinitionDataManager {

    public MybatisProcessDefinitionDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public Class<? extends ProcessDefinitionEntity> getManagedEntityClass() {
        return ProcessDefinitionEntityImpl.class;
    }

    @Override
    public ProcessDefinitionEntity create() {
        return new ProcessDefinitionEntityImpl();
    }

    @Override
    public ProcessDefinitionEntity findLatestProcessDefinitionByKey(String processDefinitionKey) {
        return (ProcessDefinitionEntity) getDbSqlSession().selectOne("selectLatestProcessDefinitionByKey", processDefinitionKey);
    }

    @Override
    public ProcessDefinitionEntity findLatestProcessDefinitionByKeyAndTenantId(String processDefinitionKey, String tenantId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("processDefinitionKey", processDefinitionKey);
        params.put("tenantId", tenantId);
        return (ProcessDefinitionEntity) getDbSqlSession().selectOne("selectLatestProcessDefinitionByKeyAndTenantId", params);
    }
    
    @Override
    public ProcessDefinitionEntity findLatestDerivedProcessDefinitionByKey(String processDefinitionKey) {
        return (ProcessDefinitionEntity) getDbSqlSession().selectOne("selectLatestDerivedProcessDefinitionByKey", processDefinitionKey);
    }

    @Override
    public ProcessDefinitionEntity findLatestDerivedProcessDefinitionByKeyAndTenantId(String processDefinitionKey, String tenantId) {
        Map<String, Object> params = new HashMap<String, Object>(2);
        params.put("processDefinitionKey", processDefinitionKey);
        params.put("tenantId", tenantId);
        return (ProcessDefinitionEntity) getDbSqlSession().selectOne("selectLatestDerivedProcessDefinitionByKeyAndTenantId", params);
    }

    @Override
    public void deleteProcessDefinitionsByDeploymentId(String deploymentId) {
        getDbSqlSession().delete("deleteProcessDefinitionsByDeploymentId", deploymentId, ProcessDefinitionEntityImpl.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessDefinition> findProcessDefinitionsByQueryCriteria(ProcessDefinitionQueryImpl processDefinitionQuery) {
        return getDbSqlSession().selectList("selectProcessDefinitionsByQueryCriteria", processDefinitionQuery);
    }

    @Override
    public long findProcessDefinitionCountByQueryCriteria(ProcessDefinitionQueryImpl processDefinitionQuery) {
        return (Long) getDbSqlSession().selectOne("selectProcessDefinitionCountByQueryCriteria", processDefinitionQuery);
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByDeploymentAndKey(String deploymentId, String processDefinitionKey) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deploymentId", deploymentId);
        parameters.put("processDefinitionKey", processDefinitionKey);
        return (ProcessDefinitionEntity) getDbSqlSession().selectOne("selectProcessDefinitionByDeploymentAndKey", parameters);
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String processDefinitionKey, String tenantId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deploymentId", deploymentId);
        parameters.put("processDefinitionKey", processDefinitionKey);
        parameters.put("tenantId", tenantId);
        return (ProcessDefinitionEntity) getDbSqlSession().selectOne("selectProcessDefinitionByDeploymentAndKeyAndTenantId", parameters);
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByKeyAndVersion(String processDefinitionKey, Integer processDefinitionVersion) {
        Map<String, Object> params = new HashMap<>();
        params.put("processDefinitionKey", processDefinitionKey);
        params.put("processDefinitionVersion", processDefinitionVersion);
        List<ProcessDefinitionEntity> results = getDbSqlSession().selectList("selectProcessDefinitionsByKeyAndVersion", params);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("There are " + results.size() + " process definitions with key = '" + processDefinitionKey + "' and version = '" + processDefinitionVersion + "'.");
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ProcessDefinitionEntity findProcessDefinitionByKeyAndVersionAndTenantId(String processDefinitionKey, Integer processDefinitionVersion, String tenantId) {
        Map<String, Object> params = new HashMap<>();
        params.put("processDefinitionKey", processDefinitionKey);
        params.put("processDefinitionVersion", processDefinitionVersion);
        params.put("tenantId", tenantId);
        List<ProcessDefinitionEntity> results = getDbSqlSession().selectList("selectProcessDefinitionsByKeyAndVersionAndTenantId", params);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("There are " + results.size() + " process definitions with key = '" + processDefinitionKey + "' and version = '" + processDefinitionVersion + "'.");
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessDefinition> findProcessDefinitionsByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectProcessDefinitionByNativeQuery", parameterMap);
    }

    @Override
    public long findProcessDefinitionCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectProcessDefinitionCountByNativeQuery", parameterMap);
    }

    @Override
    public void updateProcessDefinitionTenantIdForDeployment(String deploymentId, String newTenantId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().update("updateProcessDefinitionTenantIdForDeploymentId", params);
    }

}
