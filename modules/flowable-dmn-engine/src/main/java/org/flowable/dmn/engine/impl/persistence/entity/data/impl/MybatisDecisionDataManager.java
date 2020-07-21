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
package org.flowable.dmn.engine.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.DecisionQueryImpl;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityImpl;
import org.flowable.dmn.engine.impl.persistence.entity.data.AbstractDmnDataManager;
import org.flowable.dmn.engine.impl.persistence.entity.data.DecisionDataManager;

/**
 * @author Joram Barrez
 * @author Yvo Swillens
 */
public class MybatisDecisionDataManager extends AbstractDmnDataManager<DecisionEntity> implements DecisionDataManager {

    public MybatisDecisionDataManager(DmnEngineConfiguration dmnEngineConfiguration) {
        super(dmnEngineConfiguration);
    }

    @Override
    public Class<? extends DecisionEntity> getManagedEntityClass() {
        return DecisionEntityImpl.class;
    }

    @Override
    public DecisionEntity create() {
        return new DecisionEntityImpl();
    }

    @Override
    public DecisionEntity findLatestDecisionByKey(String decisionKey) {
        return (DecisionEntity) getDbSqlSession().selectOne("selectLatestDecisionByKey", decisionKey);
    }

    @Override
    public DecisionEntity findLatestDecisionByKeyAndTenantId(String decisionKey, String tenantId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("decisionKey", decisionKey);
        params.put("tenantId", tenantId);
        return (DecisionEntity) getDbSqlSession().selectOne("selectLatestDecisionByKeyAndTenantId", params);
    }

    @Override
    public DecisionEntity findLatestDecisionByKeyAndParentDeploymentId(String decisionKey, String parentDeploymentId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("decisionKey", decisionKey);
        params.put("parentDeploymentId", parentDeploymentId);
        return (DecisionEntity) getDbSqlSession().selectOne("selectLatestDecisionByKeyAndParentDeploymentId", params);
    }

    @Override
    public DecisionEntity findLatestDecisionByKeyParentDeploymentIdAndTenantId(String decisionKey, String parentDeploymentId, String tenantId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("decisionKey", decisionKey);
        params.put("parentDeploymentId", parentDeploymentId);
        params.put("tenantId", tenantId);
        return (DecisionEntity) getDbSqlSession().selectOne("selectLatestDecisionByKeyParentDeploymentIdAndTenantId", params);
    }

    @Override
    public void deleteDecisionsByDeploymentId(String deploymentId) {
        getDbSqlSession().delete("deleteDecisionsByDeploymentId", deploymentId, getManagedEntityClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DmnDecision> findDecisionsByQueryCriteria(DecisionQueryImpl DecisionQuery) {
        return getDbSqlSession().selectList("selectDecisionsByQueryCriteria", DecisionQuery);
    }

    @Override
    public long findDecisionCountByQueryCriteria(DecisionQueryImpl DecisionQuery) {
        return (Long) getDbSqlSession().selectOne("selectDecisionCountByQueryCriteria", DecisionQuery);
    }

    @Override
    public DecisionEntity findDecisionByDeploymentAndKey(String deploymentId, String decisionKey) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deploymentId", deploymentId);
        parameters.put("decisionKey", decisionKey);
        return (DecisionEntity) getDbSqlSession().selectOne("selectDecisionByDeploymentAndKey", parameters);
    }

    @Override
    public DecisionEntity findDecisionByDeploymentAndKeyAndTenantId(String deploymentId, String decisionKey, String tenantId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("deploymentId", deploymentId);
        parameters.put("decisionKey", decisionKey);
        parameters.put("tenantId", tenantId);
        return (DecisionEntity) getDbSqlSession().selectOne("selectDecisionByDeploymentAndKeyAndTenantId", parameters);
    }

    @Override
    public DecisionEntity findDecisionByKeyAndVersion(String decisionKey, Integer decisionVersion) {
        Map<String, Object> params = new HashMap<>();
        params.put("decisionKey", decisionKey);
        params.put("decisionVersion", decisionVersion);
        List<DecisionEntity> results = getDbSqlSession().selectList("selectDecisionsByKeyAndVersion", params);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("There are " + results.size() + " decision tables with key = '" + decisionKey + "' and version = '" + decisionVersion + "'.");
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public DecisionEntity findDecisionByKeyAndVersionAndTenantId(String decisionKey, Integer decisionVersion, String tenantId) {
        Map<String, Object> params = new HashMap<>();
        params.put("decisionKey", decisionKey);
        params.put("decisionVersion", decisionVersion);
        params.put("tenantId", tenantId);
        List<DecisionEntity> results = getDbSqlSession().selectList("selectDecisionsByKeyAndVersionAndTenantId", params);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            throw new FlowableException("There are " + results.size() + " decisions with key = '" + decisionKey + "' and version = '" + decisionVersion + "'.");
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DmnDecision> findDecisionsByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectDecisionByNativeQuery", parameterMap);
    }

    @Override
    public long findDecisionCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectDecisionCountByNativeQuery", parameterMap);
    }

    @Override
    public void updateDecisionTenantIdForDeployment(String deploymentId, String newTenantId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().update("updateDecisionTenantIdForDeploymentId", params);
    }

}
