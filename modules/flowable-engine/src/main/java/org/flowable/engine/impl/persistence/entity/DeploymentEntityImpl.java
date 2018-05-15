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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class DeploymentEntityImpl extends AbstractEntityNoRevision implements DeploymentEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String category;
    protected String key;
    protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;
    protected Map<String, EngineResource> resources;
    protected Date deploymentTime;
    protected boolean isNew;
    protected String derivedFrom;
    protected String derivedFromRoot;
    protected String parentDeploymentId;

    // Backwards compatibility
    protected String engineVersion;

    /**
     * Will only be used during actual deployment to pass deployed artifacts (eg process definitions). Will be null otherwise.
     */
    protected Map<Class<?>, List<Object>> deployedArtifacts;

    public DeploymentEntityImpl() {

    }

    @Override
    public void addResource(ResourceEntity resource) {
        if (resources == null) {
            resources = new HashMap<>();
        }
        resources.put(resource.getName(), resource);
    }

    // lazy loading ///////////////////////////////////////////////////////////////

    @Override
    public Map<String, EngineResource> getResources() {
        if (resources == null && id != null) {
            List<ResourceEntity> resourcesList = CommandContextUtil.getResourceEntityManager().findResourcesByDeploymentId(id);
            resources = new HashMap<>();
            for (ResourceEntity resource : resourcesList) {
                resources.put(resource.getName(), resource);
            }
        }
        return resources;
    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("category", this.category);
        persistentState.put("key", this.key);
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
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
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
    public void setResources(Map<String, EngineResource> resources) {
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

    @Override
    public String getEngineVersion() {
        return engineVersion;
    }

    @Override
    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }
    
    @Override
    public String getDerivedFrom() {
        return derivedFrom;
    }

    @Override
    public void setDerivedFrom(String derivedFrom) {
        this.derivedFrom = derivedFrom;
    }

    @Override
    public String getDerivedFromRoot() {
        return derivedFromRoot;
    }

    @Override
    public void setDerivedFromRoot(String derivedFromRoot) {
        this.derivedFromRoot = derivedFromRoot;
    }
    
    @Override
    public String getParentDeploymentId() {
        return parentDeploymentId;
    }

    @Override
    public void setParentDeploymentId(String parentDeploymentId) {
        this.parentDeploymentId = parentDeploymentId;
    }

    // common methods //////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return "DeploymentEntity[id=" + id + ", name=" + name + "]";
    }

}
