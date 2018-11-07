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

import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.persistence.entity.DmnResourceEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DmnResourceEntityImpl;
import org.flowable.dmn.engine.impl.persistence.entity.data.AbstractDmnDataManager;
import org.flowable.dmn.engine.impl.persistence.entity.data.DmnResourceDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisDmnResourceDataManager extends AbstractDmnDataManager<DmnResourceEntity> implements DmnResourceDataManager {

    public MybatisDmnResourceDataManager(DmnEngineConfiguration dmnEngineConfiguration) {
        super(dmnEngineConfiguration);
    }

    @Override
    public Class<? extends DmnResourceEntity> getManagedEntityClass() {
        return DmnResourceEntityImpl.class;
    }

    @Override
    public DmnResourceEntity create() {
        return new DmnResourceEntityImpl();
    }

    @Override
    public void deleteResourcesByDeploymentId(String deploymentId) {
        getDbSqlSession().delete("deleteDmnResourcesByDeploymentId", deploymentId, getManagedEntityClass());
    }

    @Override
    public DmnResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName) {
        Map<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("resourceName", resourceName);
        return (DmnResourceEntity) getDbSqlSession().selectOne("selectDmnResourceByDeploymentIdAndResourceName", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DmnResourceEntity> findResourcesByDeploymentId(String deploymentId) {
        return getDbSqlSession().selectList("selectDmnResourcesByDeploymentId", deploymentId);
    }

}
