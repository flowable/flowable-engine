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

package org.flowable.form.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.persistence.entity.data.FormResourceDataManager;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class FormResourceEntityManagerImpl extends AbstractEntityManager<FormResourceEntity> implements FormResourceEntityManager {

    protected FormResourceDataManager resourceDataManager;

    public FormResourceEntityManagerImpl(FormEngineConfiguration dmnEngineConfiguration, FormResourceDataManager resourceDataManager) {
        super(dmnEngineConfiguration);
        this.resourceDataManager = resourceDataManager;
    }

    @Override
    protected DataManager<FormResourceEntity> getDataManager() {
        return resourceDataManager;
    }

    @Override
    public void deleteResourcesByDeploymentId(String deploymentId) {
        resourceDataManager.deleteResourcesByDeploymentId(deploymentId);
    }

    @Override
    public FormResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName) {
        return resourceDataManager.findResourceByDeploymentIdAndResourceName(deploymentId, resourceName);
    }

    @Override
    public List<FormResourceEntity> findResourcesByDeploymentId(String deploymentId) {
        return resourceDataManager.findResourcesByDeploymentId(deploymentId);
    }

    public FormResourceDataManager getResourceDataManager() {
        return resourceDataManager;
    }

    public void setResourceDataManager(FormResourceDataManager resourceDataManager) {
        this.resourceDataManager = resourceDataManager;
    }

}
