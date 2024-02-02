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

import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.persistence.entity.AppResourceEntity;
import org.flowable.app.engine.impl.persistence.entity.AppResourceEntityImpl;
import org.flowable.app.engine.impl.persistence.entity.data.AbstractAppDataManager;
import org.flowable.app.engine.impl.persistence.entity.data.AppResourceDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisResourceDataManager extends AbstractAppDataManager<AppResourceEntity> implements AppResourceDataManager {

    public MybatisResourceDataManager(AppEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public Class<? extends AppResourceEntity> getManagedEntityClass() {
        return AppResourceEntityImpl.class;
    }

    @Override
    public AppResourceEntity create() {
        return new AppResourceEntityImpl();
    }

    @Override
    public void deleteResourcesByDeploymentId(String deploymentId) {
        getDbSqlSession().delete("deleteAppResourcesByDeploymentId", deploymentId, AppResourceEntityImpl.class);
    }

    @Override
    public AppResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName) {
        Map<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("resourceName", resourceName);
        return (AppResourceEntity) getDbSqlSession().selectOne("selectAppResourceByDeploymentIdAndResourceName", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AppResourceEntity> findResourcesByDeploymentId(String deploymentId) {
        return getDbSqlSession().selectList("selectAppResourcesByDeploymentId", deploymentId);
    }

}
