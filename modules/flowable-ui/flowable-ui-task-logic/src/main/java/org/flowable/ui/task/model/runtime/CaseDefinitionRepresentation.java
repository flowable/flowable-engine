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
package org.flowable.ui.task.model.runtime;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.ui.common.model.AbstractRepresentation;

/**
 * REST representation of a case definition.
 *
 * @author Tijs Rademakers
 */
public class CaseDefinitionRepresentation extends AbstractRepresentation {

    protected String id;
    protected String name;
    protected String description;
    protected String key;
    protected String category;
    protected int version;
    protected String deploymentId;
    protected String tenantId;
    protected boolean hasStartForm;

    public CaseDefinitionRepresentation(CaseDefinition caseDefinition) {
        this.id = caseDefinition.getId();
        this.name = caseDefinition.getName();
        this.description = caseDefinition.getDescription();
        this.key = caseDefinition.getKey();
        this.category = caseDefinition.getCategory();
        this.version = caseDefinition.getVersion();
        this.deploymentId = caseDefinition.getDeploymentId();
        this.tenantId = caseDefinition.getTenantId();
        this.hasStartForm = caseDefinition.hasStartFormKey();
    }

    public CaseDefinitionRepresentation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isHasStartForm() {
        return hasStartForm;
    }

    public void setHasStartForm(boolean hasStartForm) {
        this.hasStartForm = hasStartForm;
    }
}
