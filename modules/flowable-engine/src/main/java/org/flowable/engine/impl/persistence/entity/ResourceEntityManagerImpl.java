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

package org.flowable.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.data.ResourceDataManager;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class ResourceEntityManagerImpl
    extends AbstractProcessEngineEntityManager<ResourceEntity, ResourceDataManager>
    implements ResourceEntityManager {

    public ResourceEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, ResourceDataManager resourceDataManager) {
        super(processEngineConfiguration, resourceDataManager);
    }

    @Override
    public void deleteResourcesByDeploymentId(String deploymentId) {
        dataManager.deleteResourcesByDeploymentId(deploymentId);
    }

    @Override
    public ResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName) {
        return dataManager.findResourceByDeploymentIdAndResourceName(deploymentId, resourceName);
    }

    @Override
    public List<ResourceEntity> findResourcesByDeploymentId(String deploymentId) {
        return dataManager.findResourcesByDeploymentId(deploymentId);
    }

}
