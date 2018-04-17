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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;
import org.flowable.engine.ProcessEngineConfiguration;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ModelEntityImpl extends AbstractEntity implements ModelEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String key;
    protected String category;
    protected Date createTime;
    protected Date lastUpdateTime;
    protected Integer version = 1;
    protected String metaInfo;
    protected String deploymentId;
    protected String editorSourceValueId;
    protected String editorSourceExtraValueId;
    protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;

    public ModelEntityImpl() {

    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("name", this.name);
        persistentState.put("key", key);
        persistentState.put("category", this.category);
        persistentState.put("createTime", this.createTime);
        persistentState.put("lastUpdateTime", lastUpdateTime);
        persistentState.put("version", this.version);
        persistentState.put("metaInfo", this.metaInfo);
        persistentState.put("deploymentId", deploymentId);
        persistentState.put("editorSourceValueId", this.editorSourceValueId);
        persistentState.put("editorSourceExtraValueId", this.editorSourceExtraValueId);
        persistentState.put("tenantId", this.tenantId);
        return persistentState;
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
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(String key) {
        this.key = key;
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
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Override
    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String getMetaInfo() {
        return metaInfo;
    }

    @Override
    public void setMetaInfo(String metaInfo) {
        this.metaInfo = metaInfo;
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
    public String getEditorSourceValueId() {
        return editorSourceValueId;
    }

    @Override
    public void setEditorSourceValueId(String editorSourceValueId) {
        this.editorSourceValueId = editorSourceValueId;
    }

    @Override
    public String getEditorSourceExtraValueId() {
        return editorSourceExtraValueId;
    }

    @Override
    public void setEditorSourceExtraValueId(String editorSourceExtraValueId) {
        this.editorSourceExtraValueId = editorSourceExtraValueId;
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
    public boolean hasEditorSource() {
        return this.editorSourceValueId != null;
    }

    @Override
    public boolean hasEditorSourceExtra() {
        return this.editorSourceExtraValueId != null;
    }

}
