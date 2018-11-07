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

import java.util.List;
import java.util.Map;

import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.DmnDeploymentQueryImpl;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntityImpl;
import org.flowable.dmn.engine.impl.persistence.entity.data.AbstractDmnDataManager;
import org.flowable.dmn.engine.impl.persistence.entity.data.DmnDeploymentDataManager;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class MybatisDmnDeploymentDataManager extends AbstractDmnDataManager<DmnDeploymentEntity> implements DmnDeploymentDataManager {

    public MybatisDmnDeploymentDataManager(DmnEngineConfiguration dmnEngineConfiguration) {
        super(dmnEngineConfiguration);
    }

    @Override
    public Class<? extends DmnDeploymentEntity> getManagedEntityClass() {
        return DmnDeploymentEntityImpl.class;
    }

    @Override
    public DmnDeploymentEntity create() {
        return new DmnDeploymentEntityImpl();
    }

    @Override
    public long findDeploymentCountByQueryCriteria(DmnDeploymentQueryImpl deploymentQuery) {
        return (Long) getDbSqlSession().selectOne("selectDmnDeploymentCountByQueryCriteria", deploymentQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DmnDeployment> findDeploymentsByQueryCriteria(DmnDeploymentQueryImpl deploymentQuery) {
        return getDbSqlSession().selectList("selectDmnDeploymentsByQueryCriteria", deploymentQuery);
    }

    @Override
    public List<String> getDeploymentResourceNames(String deploymentId) {
        return getDbSqlSession().getSqlSession().selectList("selectDmnResourceNamesByDeploymentId", deploymentId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DmnDeployment> findDeploymentsByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectDmnDeploymentByNativeQuery", parameterMap);
    }

    @Override
    public long findDeploymentCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectDmnDeploymentCountByNativeQuery", parameterMap);
    }

}
