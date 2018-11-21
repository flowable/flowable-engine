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
package org.flowable.entitylink.service.impl.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Tijs Rademakers
 */
public class EntityLinkEntityImpl extends AbstractEntityLinkServiceNoRevisionEntity implements EntityLinkEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String linkType;
    protected String scopeId;
    protected String scopeType;
    protected String scopeDefinitionId;
    protected String referenceScopeId;
    protected String referenceScopeType;
    protected String referenceScopeDefinitionId;
    protected String rootScopeId;
    protected String rootScopeType;
    protected String rootScopeDefinitionId;
    protected Date createTime;

    public EntityLinkEntityImpl() {

    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("linkType", this.linkType);
        persistentState.put("scopeId", this.scopeId);
        persistentState.put("scopeType", this.scopeType);
        persistentState.put("scopeDefinitionId", this.scopeDefinitionId);
        persistentState.put("referenceScopeId", this.referenceScopeId);
        persistentState.put("referenceScopeType", this.referenceScopeType);
        persistentState.put("referenceScopeDefinitionId", this.referenceScopeDefinitionId);
        persistentState.put("rootScopeId", this.rootScopeId);
        persistentState.put("rootScopeType", this.rootScopeType);
        persistentState.put("rootScopeDefinitionId", this.rootScopeDefinitionId);

        return persistentState;
    }
    
    @Override
    public String getLinkType() {
        return linkType;
    }
    
    @Override
    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    @Override
    public String getScopeId() {
        return this.scopeId;
    }
    
    @Override
    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    @Override
    public String getScopeType() {
        return this.scopeType;
    }
    
    @Override
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @Override
    public String getScopeDefinitionId() {
        return this.scopeDefinitionId;
    }

    @Override
    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    @Override
    public String getReferenceScopeId() {
        return referenceScopeId;
    }
    
    @Override
    public void setReferenceScopeId(String referenceScopeId) {
        this.referenceScopeId = referenceScopeId;
    }

    @Override
    public String getReferenceScopeType() {
        return referenceScopeType;
    }
    
    @Override
    public void setReferenceScopeType(String referenceScopeType) {
        this.referenceScopeType = referenceScopeType;
    }

    @Override
    public String getReferenceScopeDefinitionId() {
        return referenceScopeDefinitionId;
    }

    @Override
    public void setReferenceScopeDefinitionId(String referenceScopeDefinitionId) {
        this.referenceScopeDefinitionId = referenceScopeDefinitionId;
    }

    @Override
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String getRootScopeId() {
        return rootScopeId;
    }
    @Override
    public void setRootScopeId(String rootScopeId) {
        this.rootScopeId = rootScopeId;
    }
    @Override
    public String getRootScopeType() {
        return rootScopeType;
    }
    @Override
    public void setRootScopeType(String rootScopeType) {
        this.rootScopeType = rootScopeType;
    }
    @Override
    public String getRootScopeDefinitionId() {
        return rootScopeDefinitionId;
    }
    @Override
    public void setRootScopeDefinitionId(String rootScopeDefinitionId) {
        this.rootScopeDefinitionId = rootScopeDefinitionId;
    }
}
