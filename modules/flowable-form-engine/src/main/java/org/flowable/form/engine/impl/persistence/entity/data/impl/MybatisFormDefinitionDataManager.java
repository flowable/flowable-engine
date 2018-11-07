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
package org.flowable.form.engine.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.FormDefinitionQueryImpl;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntityImpl;
import org.flowable.form.engine.impl.persistence.entity.data.AbstractFormDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.FormDefinitionDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisFormDefinitionDataManager extends AbstractFormDataManager<FormDefinitionEntity> implements FormDefinitionDataManager {

    public MybatisFormDefinitionDataManager(FormEngineConfiguration formEngineConfiguration) {
        super(formEngineConfiguration);
    }

    @Override
    public Class<? extends FormDefinitionEntity> getManagedEntityClass() {
        return FormDefinitionEntityImpl.class;
    }

    @Override
    public FormDefinitionEntity create() {
        return new FormDefinitionEntityImpl();
    }

    @Override
    public FormDefinitionEntity findLatestFormDefinitionByKey(String formDefinitionKey) {
        return (FormDefinitionEntity) getDbSqlSession().selectOne("selectLatestFormDefinitionByKey", formDefinitionKey);
    }

    @Override
    public FormDefinitionEntity findLatestFormDefinitionByKeyAndTenantId(String formDefinitionKey, String tenantId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("formDefinitionKey", formDefinitionKey);
        params.put("tenantId", tenantId);
        return (FormDefinitionEntity) getDbSqlSession().selectOne("selectLatestFormDefinitionByKeyAndTenantId", params);
    }

    @Override
    public FormDefinitionEntity findLatestFormDefinitionByKeyAndParentDeploymentId(String formDefinitionKey, String parentDeploymentId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("formDefinitionKey", formDefinitionKey);
        params.put("parentDeploymentId", parentDeploymentId);
        return (FormDefinitionEntity) getDbSqlSession().selectOne("selectLatestFormDefinitionByKeyAndParentDeploymentId", params);
    }

    @Override
    public FormDefinitionEntity findLatestFormDefinitionByKeyParentDeploymentIdAndTenantId(String formDefinitionKey, String parentDeploymentId, String tenantId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("formDefinitionKey", formDefinitionKey);
        params.put("parentDeploymentId", parentDeploymentId);
        params.put("tenantId", tenantId);
        return (FormDefinitionEntity) getDbSqlSession().selectOne("selectLatestFormDefinitionByKeyParentDeploymentIdAndTenantId", params);
    }

    @Override
    public void deleteFormDefinitionsByDeploymentId(String deploymentId) {
        getDbSqlSession().delete("deleteFormDefinitionsByDeploymentId", deploymentId, getManagedEntityClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FormDefinition> findFormDefinitionsByQueryCriteria(FormDefinitionQueryImpl formQuery) {
        return getDbSqlSession().selectList("selectFormDefinitionsByQueryCriteria", formQuery);
    }

    @Override
    public long findFormDefinitionCountByQueryCriteria(FormDefinitionQueryImpl formQuery) {
        return (Long) getDbSqlSession().selectOne("selectFormDefinitionCountByQueryCriteria", formQuery);
    }

    @Override
    public FormDefinitionEntity findFormDefinitionByDeploymentAndKey(String deploymentId, String formDefinitionKey) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deploymentId", deploymentId);
        parameters.put("formDefinitionKey", formDefinitionKey);
        return (FormDefinitionEntity) getDbSqlSession().selectOne("selectFormDefinitionByDeploymentAndKey", parameters);
    }

    @Override
    public FormDefinitionEntity findFormDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String formDefinitionKey, String tenantId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deploymentId", deploymentId);
        parameters.put("formDefinitionKey", formDefinitionKey);
        parameters.put("tenantId", tenantId);
        return (FormDefinitionEntity) getDbSqlSession().selectOne("selectFormDefinitionByDeploymentAndKeyAndTenantId", parameters);
    }

    @Override
    public FormDefinitionEntity findFormDefinitionByKeyAndVersion(String formDefinitionKey, Integer formVersion) {
        Map<String, Object> params = new HashMap<>();
        params.put("formDefinitionKey", formDefinitionKey);
        params.put("formVersion", formVersion);
        List<FormDefinitionEntity> results = getDbSqlSession().selectList("selectFormDefinitionsByKeyAndVersion", params);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("There are " + results.size() + " forms with key = '" + formDefinitionKey + "' and version = '" + formVersion + "'.");
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public FormDefinitionEntity findFormDefinitionByKeyAndVersionAndTenantId(String formDefinitionKey, Integer formVersion, String tenantId) {
        Map<String, Object> params = new HashMap<>();
        params.put("formDefinitionKey", formDefinitionKey);
        params.put("formVersion", formVersion);
        params.put("tenantId", tenantId);
        List<FormDefinitionEntity> results = getDbSqlSession().selectList("selectFormDefinitionsByKeyAndVersionAndTenantId", params);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("There are " + results.size() + " forms with key = '" + formDefinitionKey + "' and version = '" + formVersion + "'.");
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FormDefinition> findFormDefinitionsByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectFormDefinitionByNativeQuery", parameterMap);
    }

    @Override
    public long findFormDefinitionCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectFormDefinitionCountByNativeQuery", parameterMap);
    }

    @Override
    public void updateFormDefinitionTenantIdForDeployment(String deploymentId, String newTenantId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().update("updateFormDefinitionTenantIdForDeploymentId", params);
    }

}
