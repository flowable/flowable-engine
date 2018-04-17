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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.bpmn.data.IOSpecification;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class ProcessDefinitionEntityImpl extends AbstractEntity implements ProcessDefinitionEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String description;
    protected String key;
    protected int version;
    protected String category;
    protected String deploymentId;
    protected String resourceName;
    protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;
    protected Integer historyLevel;
    protected String diagramResourceName;
    protected boolean isGraphicalNotationDefined;
    protected Map<String, Object> variables;
    protected boolean hasStartFormKey;
    protected int suspensionState = SuspensionState.ACTIVE.getStateCode();
    protected boolean isIdentityLinksInitialized;
    protected List<IdentityLinkEntity> definitionIdentityLinkEntities = new ArrayList<>();
    protected IOSpecification ioSpecification;
    protected String derivedFrom;
    protected String derivedFromRoot;
    protected int derivedVersion;

    // Backwards compatibility
    protected String engineVersion;

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("suspensionState", this.suspensionState);
        persistentState.put("category", this.category);
        return persistentState;
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public List<IdentityLinkEntity> getIdentityLinks() {
        if (!isIdentityLinksInitialized) {
            definitionIdentityLinkEntities = CommandContextUtil.getIdentityLinkService().findIdentityLinksByProcessDefinitionId(id);
            isIdentityLinksInitialized = true;
        }

        return definitionIdentityLinkEntities;
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
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public Integer getHistoryLevel() {
        return historyLevel;
    }

    @Override
    public void setHistoryLevel(Integer historyLevel) {
        this.historyLevel = historyLevel;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
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
    public String getDiagramResourceName() {
        return diagramResourceName;
    }

    @Override
    public void setDiagramResourceName(String diagramResourceName) {
        this.diagramResourceName = diagramResourceName;
    }

    @Override
    public boolean hasStartFormKey() {
        return hasStartFormKey;
    }

    @Override
    public boolean getHasStartFormKey() {
        return hasStartFormKey;
    }

    @Override
    public void setStartFormKey(boolean hasStartFormKey) {
        this.hasStartFormKey = hasStartFormKey;
    }

    @Override
    public void setHasStartFormKey(boolean hasStartFormKey) {
        this.hasStartFormKey = hasStartFormKey;
    }

    @Override
    public boolean isGraphicalNotationDefined() {
        return isGraphicalNotationDefined;
    }

    @Override
    public boolean hasGraphicalNotation() {
        return isGraphicalNotationDefined;
    }

    @Override
    public void setGraphicalNotationDefined(boolean isGraphicalNotationDefined) {
        this.isGraphicalNotationDefined = isGraphicalNotationDefined;
    }

    @Override
    public int getSuspensionState() {
        return suspensionState;
    }

    @Override
    public void setSuspensionState(int suspensionState) {
        this.suspensionState = suspensionState;
    }

    @Override
    public boolean isSuspended() {
        return suspensionState == SuspensionState.SUSPENDED.getStateCode();
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
    public int getDerivedVersion() {
        return derivedVersion;
    }

    @Override
    public void setDerivedVersion(int derivedVersion) {
        this.derivedVersion = derivedVersion;
    }

    @Override
    public String getEngineVersion() {
        return engineVersion;
    }

    @Override
    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    public IOSpecification getIoSpecification() {
        return ioSpecification;
    }

    public void setIoSpecification(IOSpecification ioSpecification) {
        this.ioSpecification = ioSpecification;
    }

    @Override
    public String toString() {
        return "ProcessDefinitionEntity[" + id + "]";
    }

}
