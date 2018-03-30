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
package org.flowable.content.rest.service.api.content;

import java.util.Date;

import org.flowable.common.rest.util.DateToStringSerializer;
import org.flowable.content.api.ContentItem;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author Tijs Rademakers
 */
public class ContentItemResponse {

    protected String id;
    protected String name;
    protected String mimeType;
    protected String taskId;
    protected String processInstanceId;
    protected String contentStoreId;
    protected String contentStoreName;
    protected boolean contentAvailable;
    protected String tenantId;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date created;
    protected String createdBy;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date lastModified;
    protected String lastModifiedBy;
    protected String url;

    public ContentItemResponse(ContentItem contentItem, String url) {
        setId(contentItem.getId());
        setName(contentItem.getName());
        setMimeType(contentItem.getMimeType());
        setTaskId(contentItem.getTaskId());
        setProcessInstanceId(contentItem.getProcessInstanceId());
        setContentStoreId(contentItem.getContentStoreId());
        setContentStoreName(contentItem.getContentStoreName());
        setContentAvailable(contentItem.isContentAvailable());
        setTenantId(contentItem.getTenantId());
        setCreated(contentItem.getCreated());
        setCreatedBy(contentItem.getCreatedBy());
        setLastModified(contentItem.getLastModified());
        setLastModifiedBy(contentItem.getLastModifiedBy());
        setUrl(url);
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
