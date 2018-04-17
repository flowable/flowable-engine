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

package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CmmnResourceDataManager;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;

/**
 * @author Joram Barrez
 */
public class CmmnResourceEntityManagerImpl extends AbstractCmmnEntityManager<CmmnResourceEntity> implements CmmnResourceEntityManager {

    protected CmmnResourceDataManager resourceDataManager;

    public CmmnResourceEntityManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration, CmmnResourceDataManager resourceDataManager) {
        super(cmmnEngineConfiguration);
        this.resourceDataManager = resourceDataManager;
    }

    @Override
    protected DataManager<CmmnResourceEntity> getDataManager() {
        return resourceDataManager;
    }

    @Override
    public void deleteResourcesByDeploymentId(String deploymentId) {
        resourceDataManager.deleteResourcesByDeploymentId(deploymentId);
    }

    @Override
    public CmmnResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName) {
        return resourceDataManager.findResourceByDeploymentIdAndResourceName(deploymentId, resourceName);
    }

    @Override
    public List<CmmnResourceEntity> findResourcesByDeploymentId(String deploymentId) {
        return resourceDataManager.findResourcesByDeploymentId(deploymentId);
    }

    public CmmnResourceDataManager getResourceDataManager() {
        return resourceDataManager;
    }

    public void setResourceDataManager(CmmnResourceDataManager resourceDataManager) {
        this.resourceDataManager = resourceDataManager;
    }

}
