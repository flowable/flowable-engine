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

package org.flowable.app.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.persistence.entity.data.AppResourceDataManager;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;

/**
 * @author Joram Barrez
 */
public class AppResourceEntityManagerImpl extends AbstractAppEntityManager<AppResourceEntity> implements AppResourceEntityManager {

    protected AppResourceDataManager resourceDataManager;

    public AppResourceEntityManagerImpl(AppEngineConfiguration cmmnEngineConfiguration, AppResourceDataManager resourceDataManager) {
        super(cmmnEngineConfiguration);
        this.resourceDataManager = resourceDataManager;
    }

    @Override
    protected DataManager<AppResourceEntity> getDataManager() {
        return resourceDataManager;
    }

    @Override
    public void deleteResourcesByDeploymentId(String deploymentId) {
        resourceDataManager.deleteResourcesByDeploymentId(deploymentId);
    }

    @Override
    public AppResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName) {
        return resourceDataManager.findResourceByDeploymentIdAndResourceName(deploymentId, resourceName);
    }

    @Override
    public List<AppResourceEntity> findResourcesByDeploymentId(String deploymentId) {
        return resourceDataManager.findResourcesByDeploymentId(deploymentId);
    }

    public AppResourceDataManager getResourceDataManager() {
        return resourceDataManager;
    }

    public void setResourceDataManager(AppResourceDataManager resourceDataManager) {
        this.resourceDataManager = resourceDataManager;
    }

}
