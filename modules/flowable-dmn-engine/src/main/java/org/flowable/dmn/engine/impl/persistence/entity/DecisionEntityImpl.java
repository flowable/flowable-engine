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
package org.flowable.dmn.engine.impl.persistence.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.flowable.dmn.engine.DmnEngineConfiguration;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public class DecisionEntityImpl extends AbstractDmnEngineNoRevisionEntity implements DecisionEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String description;
    protected String key;
    protected int version;
    protected String category;
    protected String deploymentId;
    protected String resourceName;
    protected boolean isGraphicalNotationDefined;
    protected String diagramResourceName;
    protected String tenantId = DmnEngineConfiguration.NO_TENANT_ID;
    protected String decisionType;

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("category", this.category);
        persistentState.put("tenantId", this.tenantId);
        return persistentState;
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getDeploymentId() {
        return deploymentId;
    }

    @Override
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String getResourceName() {
        return resourceName;
    }

    @Override
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public boolean hasGraphicalNotation() {
        return isGraphicalNotationDefined;
    }

    public boolean isGraphicalNotationDefined() {
        return hasGraphicalNotation();
    }

    @Override
    public void setHasGraphicalNotation(boolean hasGraphicalNotation) {
        this.isGraphicalNotationDefined = hasGraphicalNotation;
    }

    @Override
    public String getDiagramResourceName() {
        return diagramResourceName;
    }

    @Override
    public void setDiagramResourceName(String diagramResourceName) {
        this.diagramResourceName = diagramResourceName;
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
    public String getCategory() {
        return category;
    }

    @Override
    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String getDecisionType() {
        return decisionType;
    }

    @Override
    public void setDecisionType(String decisionType) {
        this.decisionType = decisionType;
    }

    @Override
    public String toString() {
        return "DecisionEntity[" + id + "]";
    }

}
