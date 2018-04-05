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
import java.util.Set;

import org.flowable.common.rest.api.PaginateRequest;

/**
 * @author Tijs Rademakers
 */
public class ContentItemQueryRequest extends PaginateRequest {

    protected String id;
    protected Set<String> ids;
    protected String name;
    protected String nameLike;
    protected String mimeType;
    protected String mimeTypeLike;
    protected String taskId;
    protected String taskIdLike;
    protected String processInstanceId;
    protected String processInstanceIdLike;
    protected String contentStoreId;
    protected String contentStoreIdLike;
    protected String contentStoreName;
    protected String contentStoreNameLike;
    protected Long contentSize;
    protected Long minimumContentSize;
    protected Long maximumContentSize;
    protected Boolean contentAvailable;
    protected String field;
    protected String fieldLike;
    protected Date createdOn;
    protected Date createdBefore;
    protected Date createdAfter;
    protected String createdBy;
    protected String createdByLike;
    protected Date lastModifiedOn;
    protected Date lastModifiedBefore;
    protected Date lastModifiedAfter;
    protected String lastModifiedBy;
    protected String lastModifiedByLike;
    protected String tenantId;
    protected String tenantIdLike;
    protected Boolean withoutTenantId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<String> getIds() {
        return ids;
    }

    public void setIds(Set<String> ids) {
        this.ids = ids;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameLike() {
        return nameLike;
    }

    public void setNameLike(String nameLike) {
        this.nameLike = nameLike;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeTypeLike() {
        return mimeTypeLike;
    }

    public void setMimeTypeLike(String mimeTypeLike) {
        this.mimeTypeLike = mimeTypeLike;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskIdLike() {
        return taskIdLike;
    }

    public void setTaskIdLike(String taskIdLike) {
        this.taskIdLike = taskIdLike;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessInstanceIdLike() {
        return processInstanceIdLike;
    }

    public void setProcessInstanceIdLike(String processInstanceIdLike) {
        this.processInstanceIdLike = processInstanceIdLike;
    }

    public String getContentStoreId() {
        return contentStoreId;
    }

    public void setContentStoreId(String contentStoreId) {
        this.contentStoreId = contentStoreId;
    }

    public String getContentStoreIdLike() {
        return contentStoreIdLike;
    }

    public void setContentStoreIdLike(String contentStoreIdLike) {
        this.contentStoreIdLike = contentStoreIdLike;
    }

    public String getContentStoreName() {
        return contentStoreName;
    }

    public void setContentStoreName(String contentStoreName) {
        this.contentStoreName = contentStoreName;
    }

    public String getContentStoreNameLike() {
        return contentStoreNameLike;
    }

    public void setContentStoreNameLike(String contentStoreNameLike) {
        this.contentStoreNameLike = contentStoreNameLike;
    }

    public Long getContentSize() {
        return contentSize;
    }

    public void setContentSize(Long contentSize) {
        this.contentSize = contentSize;
    }

    public Long getMinimumContentSize() {
        return minimumContentSize;
    }

    public void setMinimumContentSize(Long minimumContentSize) {
        this.minimumContentSize = minimumContentSize;
    }

    public Long getMaximumContentSize() {
        return maximumContentSize;
    }

    public void setMaximumContentSize(Long maximumContentSize) {
        this.maximumContentSize = maximumContentSize;
    }

    public Boolean getContentAvailable() {
        return contentAvailable;
    }

    public void setContentAvailable(Boolean contentAvailable) {
        this.contentAvailable = contentAvailable;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getFieldLike() {
        return fieldLike;
    }

    public void setFieldLike(String fieldLike) {
        this.fieldLike = fieldLike;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public Date getCreatedBefore() {
        return createdBefore;
    }

    public void setCreatedBefore(Date createdBefore) {
        this.createdBefore = createdBefore;
    }

    public Date getCreatedAfter() {
        return createdAfter;
    }

    public void setCreatedAfter(Date createdAfter) {
        this.createdAfter = createdAfter;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByLike() {
        return createdByLike;
    }

    public void setCreatedByLike(String createdByLike) {
        this.createdByLike = createdByLike;
    }

    public Date getLastModifiedOn() {
        return lastModifiedOn;
    }

    public void setLastModifiedOn(Date lastModifiedOn) {
        this.lastModifiedOn = lastModifiedOn;
    }

    public Date getLastModifiedBefore() {
        return lastModifiedBefore;
    }

    public void setLastModifiedBefore(Date lastModifiedBefore) {
        this.lastModifiedBefore = lastModifiedBefore;
    }

    public Date getLastModifiedAfter() {
        return lastModifiedAfter;
    }

    public void setLastModifiedAfter(Date lastModifiedAfter) {
        this.lastModifiedAfter = lastModifiedAfter;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getLastModifiedByLike() {
        return lastModifiedByLike;
    }

    public void setLastModifiedByLike(String lastModifiedByLike) {
        this.lastModifiedByLike = lastModifiedByLike;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    public void setTenantIdLike(String tenantIdLike) {
        this.tenantIdLike = tenantIdLike;
    }

    public Boolean getWithoutTenantId() {
        return withoutTenantId;
    }

    public void setWithoutTenantId(Boolean withoutTenantId) {
        this.withoutTenantId = withoutTenantId;
    }
}
