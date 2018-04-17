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
package org.flowable.cmmn.engine.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.AbstractCmmnDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CaseDefinitionDataManager;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionQueryImpl;
import org.flowable.common.engine.api.FlowableException;

/**
 * @author Joram Barrez
 */
public class MybatisCaseDefinitionDataManager extends AbstractCmmnDataManager<CaseDefinitionEntity> implements CaseDefinitionDataManager {

    public MybatisCaseDefinitionDataManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public Class<? extends CaseDefinitionEntity> getManagedEntityClass() {
        return CaseDefinitionEntityImpl.class;
    }

    @Override
    public CaseDefinitionEntity create() {
        return new CaseDefinitionEntityImpl();
    }

    @Override
    public CaseDefinitionEntity findLatestCaseDefinitionByKey(String caseDefinitionKey) {
        return (CaseDefinitionEntity) getDbSqlSession().selectOne("selectLatestCaseDefinitionByKey", caseDefinitionKey);
    }

    @Override
    public CaseDefinitionEntity findLatestCaseDefinitionByKeyAndTenantId(String caseDefinitionKey, String tenantId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("caseDefinitionKey", caseDefinitionKey);
        params.put("tenantId", tenantId);
        return (CaseDefinitionEntity) getDbSqlSession().selectOne("selectLatestCaseDefinitionByKeyAndTenantId", params);
    }

    @Override
    public void deleteCaseDefinitionsByDeploymentId(String deploymentId) {
        getDbSqlSession().delete("deleteCaseDefinitionsByDeploymentId", deploymentId, CaseDefinitionEntityImpl.class);
    }

    @Override
    public CaseDefinitionEntity findCaseDefinitionByDeploymentAndKey(String deploymentId, String caseDefinitionKey) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deploymentId", deploymentId);
        parameters.put("caseDefinitionKey", caseDefinitionKey);
        return (CaseDefinitionEntity) getDbSqlSession().selectOne("selectCaseDefinitionByDeploymentAndKey", parameters);
    }

    @Override
    public CaseDefinitionEntity findCaseDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String caseDefinitionKey, String tenantId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deploymentId", deploymentId);
        parameters.put("caseDefinitionKey", caseDefinitionKey);
        parameters.put("tenantId", tenantId);
        return (CaseDefinitionEntity) getDbSqlSession().selectOne("selectCaseDefinitionByDeploymentAndKeyAndTenantId", parameters);
    }

    @Override
    public CaseDefinitionEntity findCaseDefinitionByKeyAndVersion(String caseDefinitionKey, Integer caseDefinitionVersion) {
        Map<String, Object> params = new HashMap<>();
        params.put("caseDefinitionKey", caseDefinitionKey);
        params.put("caseDefinitionVersion", caseDefinitionVersion);
        List<CaseDefinitionEntity> results = getDbSqlSession().selectList("selectCaseDefinitionsByKeyAndVersion", params);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("There are " + results.size() + " case definitions with key = '" + caseDefinitionKey + "' and version = '" + caseDefinitionVersion + "'.");
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CaseDefinitionEntity findCaseDefinitionByKeyAndVersionAndTenantId(String caseDefinitionKey, Integer caseDefinitionVersion, String tenantId) {
        Map<String, Object> params = new HashMap<>();
        params.put("caseDefinitionKey", caseDefinitionKey);
        params.put("caseDefinitionVersion", caseDefinitionVersion);
        params.put("tenantId", tenantId);
        List<CaseDefinitionEntity> results = getDbSqlSession().selectList("selectCaseDefinitionsByKeyAndVersionAndTenantId", params);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("There are " + results.size() + " case definitions with key = '" + caseDefinitionKey + "' and version = '" + caseDefinitionVersion + "'.");
        }
        return null;
    }

    @Override
    public void updateCaseDefinitionTenantIdForDeployment(String deploymentId, String newTenantId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().update("updateCaseDefinitionTenantIdForDeploymentId", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CaseDefinition> findCaseDefinitionsByQueryCriteria(CaseDefinitionQueryImpl caseDefinitionQuery) {
        return getDbSqlSession().selectList("selectCaseDefinitionsByQueryCriteria", caseDefinitionQuery);
    }

    @Override
    public long findCaseDefinitionCountByQueryCriteria(CaseDefinitionQueryImpl caseDefinitionQuery) {
        return (Long) getDbSqlSession().selectOne("selectCaseDefinitionCountByQueryCriteria", caseDefinitionQuery);
    }

}
