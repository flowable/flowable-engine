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
package org.flowable.app.engine.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.persistence.entity.AppDefinitionEntity;
import org.flowable.app.engine.impl.persistence.entity.AppDefinitionEntityImpl;
import org.flowable.app.engine.impl.persistence.entity.data.AbstractAppDataManager;
import org.flowable.app.engine.impl.persistence.entity.data.AppDefinitionDataManager;
import org.flowable.app.engine.impl.repository.AppDefinitionQueryImpl;
import org.flowable.common.engine.api.FlowableException;

/**
 * @author Tijs Rademakers
 */
public class MybatisAppDefinitionDataManager extends AbstractAppDataManager<AppDefinitionEntity> implements AppDefinitionDataManager {

    public MybatisAppDefinitionDataManager(AppEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public Class<? extends AppDefinitionEntity> getManagedEntityClass() {
        return AppDefinitionEntityImpl.class;
    }

    @Override
    public AppDefinitionEntity create() {
        return new AppDefinitionEntityImpl();
    }

    @Override
    public AppDefinitionEntity findLatestAppDefinitionByKey(String appDefinitionKey) {
        return (AppDefinitionEntity) getDbSqlSession().selectOne("selectLatestAppDefinitionByKey", appDefinitionKey);
    }

    @Override
    public AppDefinitionEntity findLatestAppDefinitionByKeyAndTenantId(String appDefinitionKey, String tenantId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("appDefinitionKey", appDefinitionKey);
        params.put("tenantId", tenantId);
        return (AppDefinitionEntity) getDbSqlSession().selectOne("selectLatestAppDefinitionByKeyAndTenantId", params);
    }

    @Override
    public void deleteAppDefinitionsByDeploymentId(String deploymentId) {
        getDbSqlSession().delete("deleteAppDefinitionsByDeploymentId", deploymentId, AppDefinitionEntityImpl.class);
    }

    @Override
    public AppDefinitionEntity findAppDefinitionByDeploymentAndKey(String deploymentId, String appDefinitionKey) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deploymentId", deploymentId);
        parameters.put("appDefinitionKey", appDefinitionKey);
        return (AppDefinitionEntity) getDbSqlSession().selectOne("selectAppDefinitionByDeploymentAndKey", parameters);
    }

    @Override
    public AppDefinitionEntity findAppDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String appDefinitionKey, String tenantId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deploymentId", deploymentId);
        parameters.put("appDefinitionKey", appDefinitionKey);
        parameters.put("tenantId", tenantId);
        return (AppDefinitionEntity) getDbSqlSession().selectOne("selectAppDefinitionByDeploymentAndKeyAndTenantId", parameters);
    }

    @Override
    public AppDefinitionEntity findAppDefinitionByKeyAndVersion(String appDefinitionKey, Integer appDefinitionVersion) {
        Map<String, Object> params = new HashMap<>();
        params.put("appDefinitionKey", appDefinitionKey);
        params.put("appDefinitionVersion", appDefinitionVersion);
        List<AppDefinitionEntity> results = getDbSqlSession().selectList("selectAppDefinitionsByKeyAndVersion", params);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("There are " + results.size() + " app definitions with key = '" + appDefinitionKey + "' and version = '" + appDefinitionVersion + "'.");
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AppDefinitionEntity findAppDefinitionByKeyAndVersionAndTenantId(String appDefinitionKey, Integer appDefinitionVersion, String tenantId) {
        Map<String, Object> params = new HashMap<>();
        params.put("appDefinitionKey", appDefinitionKey);
        params.put("appDefinitionVersion", appDefinitionVersion);
        params.put("tenantId", tenantId);
        List<AppDefinitionEntity> results = getDbSqlSession().selectList("selectAppDefinitionsByKeyAndVersionAndTenantId", params);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("There are " + results.size() + " app definitions with key = '" + appDefinitionKey + "' and version = '" + appDefinitionVersion + "'.");
        }
        return null;
    }

    @Override
    public void updateAppDefinitionTenantIdForDeployment(String deploymentId, String newTenantId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().update("updateAppDefinitionTenantIdForDeploymentId", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AppDefinition> findAppDefinitionsByQueryCriteria(AppDefinitionQueryImpl appDefinitionQuery) {
        return getDbSqlSession().selectList("selectAppDefinitionsByQueryCriteria", appDefinitionQuery);
    }

    @Override
    public long findAppDefinitionCountByQueryCriteria(AppDefinitionQueryImpl appDefinitionQuery) {
        return (Long) getDbSqlSession().selectOne("selectAppDefinitionCountByQueryCriteria", appDefinitionQuery);
    }

}
