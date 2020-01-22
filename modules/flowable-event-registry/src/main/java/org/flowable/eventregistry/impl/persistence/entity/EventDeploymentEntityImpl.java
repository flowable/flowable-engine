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

package org.flowable.eventregistry.impl.persistence.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class EventDeploymentEntityImpl extends AbstractEventRegistryNoRevisionEntity implements EventDeploymentEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String category;
    protected String tenantId = EventRegistryEngineConfiguration.NO_TENANT_ID;
    protected String parentDeploymentId;
    protected Map<String, EventResourceEntity> resources;
    protected Date deploymentTime;
    protected boolean isNew;

    /**
     * Will only be used during actual deployment to pass deployed artifacts (eg form definitions). Will be null otherwise.
     */
    protected Map<Class<?>, List<Object>> deployedArtifacts;

    public EventDeploymentEntityImpl() {

    }

    @Override
    public void addResource(EventResourceEntity resource) {
        if (resources == null) {
            resources = new HashMap<>();
        }
        resources.put(resource.getName(), resource);
    }

    @Override
    public Map<String, EventResourceEntity> getResources() {
        return resources;
    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("category", this.category);
        persistentState.put("tenantId", tenantId);
        persistentState.put("parentDeploymentId", parentDeploymentId);
        return persistentState;
    }

    // Deployed artifacts manipulation ////////////////////////////////////////////

    @Override
    public void addDeployedArtifact(Object deployedArtifact) {
        if (deployedArtifacts == null) {
            deployedArtifacts = new HashMap<>();
        }

        Class<?> clazz = deployedArtifact.getClass();
        List<Object> artifacts = deployedArtifacts.get(clazz);
        if (artifacts == null) {
            artifacts = new ArrayList<>();
            deployedArtifacts.put(clazz, artifacts);
        }

        artifacts.add(deployedArtifact);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getDeployedArtifacts(Class<T> clazz) {
        for (Class<?> deployedArtifactsClass : deployedArtifacts.keySet()) {
            if (clazz.isAssignableFrom(deployedArtifactsClass)) {
                return (List<T>) deployedArtifacts.get(deployedArtifactsClass);
            }
        }
        return null;
    }

    // getters and setters ////////////////////////////////////////////////////////

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String getParentDeploymentId() {
        return parentDeploymentId;
    }

    @Override
    public void setParentDeploymentId(String parentDeploymentId) {
        this.parentDeploymentId = parentDeploymentId;
    }

    @Override
    public void setResources(Map<String, EventResourceEntity> resources) {
        this.resources = resources;
    }

    @Override
    public Date getDeploymentTime() {
        return deploymentTime;
    }

    @Override
    public void setDeploymentTime(Date deploymentTime) {
        this.deploymentTime = deploymentTime;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    // common methods //////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return "EventDeploymentEntity[id=" + id + ", name=" + name + "]";
    }

}
