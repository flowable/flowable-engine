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
package org.flowable.compatibility.wrapper;

import java.util.Date;
import java.util.Map;

import org.activiti.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.repository.Deployment;

/**
 * @author Joram Barrez
 */
public class Flowable5DeploymentWrapper implements Deployment {

    protected org.activiti.engine.repository.Deployment activiti5Deployment;

    public Flowable5DeploymentWrapper(org.activiti.engine.repository.Deployment activiti5Deployment) {
        this.activiti5Deployment = activiti5Deployment;
    }

    @Override
    public String getId() {
        return activiti5Deployment.getId();
    }

    @Override
    public String getName() {
        return activiti5Deployment.getName();
    }

    @Override
    public Date getDeploymentTime() {
        return activiti5Deployment.getDeploymentTime();
    }

    @Override
    public String getCategory() {
        return activiti5Deployment.getCategory();
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public String getTenantId() {
        return activiti5Deployment.getTenantId();
    }
    
    @Override
    public boolean isNew() {
        return ((DeploymentEntity) activiti5Deployment).isNew();
    }

    @Override
    public Map<String, EngineResource> getResources() {
        return null;
    }
    
    @Override
    public String getDerivedFrom() {
        return null;
    }

    @Override
    public String getDerivedFromRoot() {
        return null;
    }
    
    @Override
    public String getParentDeploymentId() {
        return null;
    }

    @Override
    public String getEngineVersion() {
        return Flowable5Util.V5_ENGINE_TAG;
    }

    public org.activiti.engine.repository.Deployment getRawObject() {
        return activiti5Deployment;
    }

}
