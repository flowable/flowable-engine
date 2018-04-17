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

import java.util.HashMap;
import java.util.Map;

import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;

public class AppDefinitionEntityImpl extends AbstractEntity implements AppDefinitionEntity {
    
    protected String category;
    protected String name;
    protected String key;
    protected String description;
    protected int version;
    protected String resourceName;
    protected String deploymentId;
    protected String tenantId = AppEngineConfiguration.NO_TENANT_ID;
    
    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("category", this.category);
        return persistentState;
    }

    @Override
    public String getCategory() {
        return category;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public String getResourceName() {
        return resourceName;
    }

    @Override
    public String getDeploymentId() {
        return deploymentId;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
}
