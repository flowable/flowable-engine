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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;

public class CaseDefinitionEntityImpl extends AbstractCmmnEngineEntity implements CaseDefinitionEntity {
    
    protected String category;
    protected String name;
    protected String key;
    protected String description;
    protected int version;
    protected String resourceName;
    protected boolean isGraphicalNotationDefined;
    protected String diagramResourceName;
    protected String deploymentId;
    protected boolean hasStartFormKey;
    protected String tenantId = CmmnEngineConfiguration.NO_TENANT_ID;
    protected boolean isIdentityLinksInitialized;
    protected List<IdentityLinkEntity> definitionIdentityLinkEntities = new ArrayList<>();
    protected String localizedName;
    protected String localizedDescription;
    
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
        if (StringUtils.isNotBlank(localizedName)) {
            return localizedName;
        }
        return name;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getDescription() {
        if (StringUtils.isNotBlank(localizedDescription)) {
            return localizedDescription;
        }
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
    public boolean hasGraphicalNotation() {
        return isGraphicalNotationDefined;
    }
    
    public boolean isGraphicalNotationDefined() {
        return hasGraphicalNotation();
    }
    
    @Override
    public String getDiagramResourceName() {
        return diagramResourceName;
    }

    @Override
    public boolean hasStartFormKey() {
        return hasStartFormKey;
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
    public void setHasGraphicalNotation(boolean hasGraphicalNotation) {
        this.isGraphicalNotationDefined = hasGraphicalNotation;
    }
    
    @Override
    public void setDiagramResourceName(String diagramResourceName) {
        this.diagramResourceName = diagramResourceName;
    }

    @Override
    public void setHasStartFormKey(boolean hasStartFormKey) {
        this.hasStartFormKey = hasStartFormKey;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    @Override
    public List<IdentityLinkEntity> getIdentityLinks() {
        if (!isIdentityLinksInitialized) {
            definitionIdentityLinkEntities = CommandContextUtil.getCmmnEngineConfiguration().getIdentityLinkServiceConfiguration()
                    .getIdentityLinkService().findIdentityLinksByScopeDefinitionIdAndType(id, ScopeTypes.CMMN);
            isIdentityLinksInitialized = true;
        }

        return definitionIdentityLinkEntities;
    }
    

    public String getLocalizedName() {
        return localizedName;
    }

    @Override
    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    public String getLocalizedDescription() {
        return localizedDescription;
    }

    @Override
    public void setLocalizedDescription(String localizedDescription) {
        this.localizedDescription = localizedDescription;
    }
}
