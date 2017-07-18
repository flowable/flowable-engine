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

import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.engine.common.impl.persistence.entity.AbstractEntityNoRevision;

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
        return persistentState;
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getContentStoreId() {
        return contentStoreId;
    }

    public void setContentStoreId(String contentStoreId) {
        this.contentStoreId = contentStoreId;
    }

    public String getContentStoreName() {
        return contentStoreName;
    }

    public void setContentStoreName(String contentStoreName) {
        this.contentStoreName = contentStoreName;
    }

    public boolean isContentAvailable() {
        return contentAvailable;
    }

    public void setContentAvailable(boolean contentAvailable) {
        this.contentAvailable = contentAvailable;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Long getContentSize() {
        return contentSize;
    }

    public void setContentSize(Long contentSize) {
        this.contentSize = contentSize;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String toString() {
        return "ContentItemEntity[" + id + "]";
    }

}
