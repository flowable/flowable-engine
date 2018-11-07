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

import java.util.List;

import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.AbstractCmmnDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CmmnDeploymentDataManager;
import org.flowable.cmmn.engine.impl.repository.CmmnDeploymentQueryImpl;

/**
 * @author Joram Barrez
 */
public class MybatisCmmnDeploymentDataManager extends AbstractCmmnDataManager<CmmnDeploymentEntity> implements CmmnDeploymentDataManager {

    public MybatisCmmnDeploymentDataManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public Class<? extends CmmnDeploymentEntity> getManagedEntityClass() {
        return CmmnDeploymentEntityImpl.class;
    }

    @Override
    public CmmnDeploymentEntity create() {
        return new CmmnDeploymentEntityImpl();
    }

    @Override
    public CmmnDeploymentEntity findLatestDeploymentByName(String deploymentName) {
        List<?> list = getDbSqlSession().selectList("selectCmmnDeploymentsByName", deploymentName, 0, 1);
        if (list != null && !list.isEmpty()) {
            return (CmmnDeploymentEntity) list.get(0);
        }
        return null;
    }

    @Override
    public List<String> getDeploymentResourceNames(String deploymentId) {
        return getDbSqlSession().getSqlSession().selectList("selectCmmnResourceNamesByDeploymentId", deploymentId);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<CmmnDeployment> findDeploymentsByQueryCriteria(CmmnDeploymentQueryImpl deploymentQuery) {
        return getDbSqlSession().selectList("selectCmmnDeploymentsByQueryCriteria", deploymentQuery);
    }
    
    @Override
    public long findDeploymentCountByQueryCriteria(CmmnDeploymentQueryImpl deploymentQuery) {
        return (Long) getDbSqlSession().selectOne("selectCmmnDeploymentCountByQueryCriteria", deploymentQuery);
    }

}
