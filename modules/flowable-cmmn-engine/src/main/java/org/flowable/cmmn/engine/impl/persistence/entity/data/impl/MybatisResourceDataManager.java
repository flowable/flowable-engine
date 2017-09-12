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

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.AbstractCmmnDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CmmnResourceDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisResourceDataManager extends AbstractCmmnDataManager<CmmnResourceEntity> implements CmmnResourceDataManager {

    public MybatisResourceDataManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public Class<? extends CmmnResourceEntity> getManagedEntityClass() {
        return CmmnResourceEntityImpl.class;
    }

    @Override
    public CmmnResourceEntity create() {
        return new CmmnResourceEntityImpl();
    }

    @Override
    public void deleteResourcesByDeploymentId(String deploymentId) {
        getDbSqlSession().delete("deleteCmmnResourcesByDeploymentId", deploymentId, CmmnResourceEntityImpl.class);
    }

    @Override
    public CmmnResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName) {
        Map<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("resourceName", resourceName);
        return (CmmnResourceEntity) getDbSqlSession().selectOne("selectCmmnResourceByDeploymentIdAndResourceName", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CmmnResourceEntity> findResourcesByDeploymentId(String deploymentId) {
        return getDbSqlSession().selectList("selectCmmnResourcesByDeploymentId", deploymentId);
    }

}
