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
package org.flowable.content.engine.impl.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;
import org.flowable.content.engine.ContentEngineConfiguration;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class ContentItemEntityImpl extends AbstractEntityNoRevision implements ContentItemEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String mimeType;
    protected String taskId;
    protected String processInstanceId;
    protected String scopeId;
    protected String scopeType;
    protected String contentStoreId;
    protected String contentStoreName;
    protected boolean contentAvailable;
    protected String field;
    protected Long contentSize;
    protected Date created;
    protected String createdBy;
    protected Date lastModified;
    protected String lastModifiedBy;
    protected String tenantId = ContentEngineConfiguration.NO_TENANT_ID;
    protected boolean provisional;

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("name", this.name);
        persistentState.put("mimeType", this.mimeType);
        persistentState.put("taskId", this.taskId);
        persistentState.put("processInstanceId", this.processInstanceId);
        persistentState.put("contentStoreId", this.contentStoreId);
        persistentState.put("contentStoreName", this.contentStoreName);
        persistentState.put("contentAvailable", this.contentAvailable);
        persistentState.put("field", this.field);
        persistentState.put("contentSize", this.contentSize);
        persistentState.put("created", this.created);
        persistentState.put("createdBy", this.createdBy);
        persistentState.put("lastModified", this.lastModified);
        persistentState.put("lastModifiedBy", this.lastModifiedBy);
        persistentState.put("tenantId", this.tenantId);
        persistentState.put("scopeId", this.scopeId);
        persistentState.put("scopeType", this.scopeType);
        return persistentState;
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

    @Override
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String getContentStoreId() {
        return contentStoreId;
    }

    @Override
    public void setContentStoreId(String contentStoreId) {
        this.contentStoreId = contentStoreId;
    }

    @Override
    public String getContentStoreName() {
        return contentStoreName;
    }

    @Override
    public void setContentStoreName(String contentStoreName) {
        this.contentStoreName = contentStoreName;
    }

    @Override
    public boolean isContentAvailable() {
        return contentAvailable;
    }

    @Override
    public void setContentAvailable(boolean contentAvailable) {
        this.contentAvailable = contentAvailable;
    }

    @Override
    public String getField() {
        return field;
    }

    @Override
    public void setField(String field) {
        this.field = field;
    }

    @Override
    public Long getContentSize() {
        return contentSize;
    }

    @Override
    public void setContentSize(Long contentSize) {
        this.contentSize = contentSize;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    @Override
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
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
    public boolean isProvisional() {
        return provisional;
    }

    @Override
    public void setProvisional(boolean provisional) {
        this.provisional = provisional;
    }

    @Override
    public String toString() {
        return "ContentItemEntity[" + id + "]";
    }

}
