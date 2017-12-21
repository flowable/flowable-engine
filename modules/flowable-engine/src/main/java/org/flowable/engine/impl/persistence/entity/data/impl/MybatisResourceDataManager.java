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

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ResourceEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.AbstractProcessDataManager;
import org.flowable.engine.impl.persistence.entity.data.ResourceDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisResourceDataManager extends AbstractProcessDataManager<ResourceEntity> implements ResourceDataManager {

    public MybatisResourceDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public Class<? extends ResourceEntity> getManagedEntityClass() {
        return ResourceEntityImpl.class;
    }

    @Override
    public ResourceEntity create() {
        return new ResourceEntityImpl();
    }

    @Override
    public void deleteResourcesByDeploymentId(String deploymentId) {
        getDbSqlSession().delete("deleteResourcesByDeploymentId", deploymentId, ResourceEntityImpl.class);
    }

    @Override
    public ResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName) {
        Map<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("resourceName", resourceName);
        return (ResourceEntity) getDbSqlSession().selectOne("selectResourceByDeploymentIdAndResourceName", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ResourceEntity> findResourcesByDeploymentId(String deploymentId) {
        return getDbSqlSession().selectList("selectResourcesByDeploymentId", deploymentId);
    }

}
