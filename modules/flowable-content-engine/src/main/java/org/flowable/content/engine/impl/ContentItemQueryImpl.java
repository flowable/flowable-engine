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

package org.flowable.content.engine.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentItemQuery;
import org.flowable.content.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ContentItemQueryImpl extends AbstractQuery<ContentItemQuery, ContentItem> implements ContentItemQuery, Serializable {

    private static final long serialVersionUID = 1L;
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
    protected String scopeId;
    protected String scopeIdLike;
    protected String scopeType;
    protected String scopeTypeLike;
    protected String contentStoreId;
    protected String contentStoreIdLike;
    protected String contentStoreName;
    protected String contentStoreNameLike;
    protected String field;
    protected String fieldLike;
    protected Boolean contentAvailable;
    protected Long contentSize;
    protected Long minContentSize;
    protected Long maxContentSize;
    protected Date createdDate;
    protected Date createdDateBefore;
    protected Date createdDateAfter;
    protected String createdBy;
    protected String createdByLike;
    protected Date lastModifiedDate;
    protected Date lastModifiedDateBefore;
    protected Date lastModifiedDateAfter;
    protected String lastModifiedBy;
    protected String lastModifiedByLike;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;

    public ContentItemQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public ContentItemQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public ContentItemQueryImpl id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public ContentItemQueryImpl ids(Set<String> ids) {
        this.ids = ids;
        return this;
    }

    @Override
    public ContentItemQueryImpl name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public ContentItemQueryImpl nameLike(String nameLike) {
        this.nameLike = nameLike;
        return this;
    }

    @Override
    public ContentItemQueryImpl mimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    @Override
    public ContentItemQueryImpl mimeTypeLike(String mimeTypeLike) {
        this.mimeTypeLike = mimeTypeLike;
        return this;
    }

    @Override
    public ContentItemQueryImpl taskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    @Override
    public ContentItemQueryImpl taskIdLike(String taskIdLike) {
        this.taskIdLike = taskIdLike;
        return this;
    }

    @Override
    public ContentItemQueryImpl processInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public ContentItemQueryImpl processInstanceIdLike(String processInstanceIdLike) {
        this.processInstanceIdLike = processInstanceIdLike;
        return this;
    }

    @Override
    public ContentItemQueryImpl scopeId(String scopeId) {
        this.scopeId = scopeId;
        return this;
    }

    @Override
    public ContentItemQueryImpl scopeIdLike(String scopeIdLike) {
        this.scopeIdLike = scopeIdLike;
        return this;
    }

    @Override
    public ContentItemQueryImpl scopeType(String scopeType) {
        this.scopeType = scopeType;
        return this;
    }

    @Override
    public ContentItemQueryImpl scopeTypeLike(String scopeTypeLike) {
        this.scopeTypeLike = scopeTypeLike;
        return this;
    }

    @Override
    public ContentItemQueryImpl field(String field) {
        this.field = field;
        return this;
    }

    @Override
    public ContentItemQueryImpl fieldLike(String fieldLike) {
        this.fieldLike = fieldLike;
        return this;
    }

    @Override
    public ContentItemQueryImpl contentStoreId(String contentStoreId) {
        this.contentStoreId = contentStoreId;
        return this;
    }

    @Override
    public ContentItemQueryImpl contentStoreIdLike(String contentStoreIdLike) {
        this.contentStoreIdLike = contentStoreIdLike;
        return this;
    }

    @Override
    public ContentItemQueryImpl contentStoreName(String contentStoreName) {
        this.contentStoreName = contentStoreName;
        return this;
    }

    @Override
    public ContentItemQueryImpl contentStoreNameLike(String contentStoreNameLike) {
        this.contentStoreNameLike = contentStoreNameLike;
        return this;
    }

    @Override
    public ContentItemQueryImpl contentAvailable(Boolean contentAvailable) {
        this.contentAvailable = contentAvailable;
        return this;
    }

    @Override
    public ContentItemQueryImpl contentSize(Long contentSize) {
        this.contentSize = contentSize;
        return this;
    }

    @Override
    public ContentItemQueryImpl minContentSize(Long minContentSize) {
        this.minContentSize = minContentSize;
        return this;
    }

    @Override
    public ContentItemQueryImpl maxContentSize(Long maxContentSize) {
        this.maxContentSize = maxContentSize;
        return this;
    }

    @Override
    public ContentItemQueryImpl createdDate(Date createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    @Override
    public ContentItemQueryImpl createdDateBefore(Date createdDateBefore) {
        this.createdDateBefore = createdDateBefore;
        return this;
    }

    @Override
    public ContentItemQueryImpl createdDateAfter(Date createdDateAfter) {
        this.createdDateAfter = createdDateAfter;
        return this;
    }

    @Override
    public ContentItemQueryImpl createdBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    @Override
    public ContentItemQueryImpl createdByLike(String createdByLike) {
        this.createdByLike = createdByLike;
        return this;
    }

    @Override
    public ContentItemQueryImpl lastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
        return this;
    }

    @Override
    public ContentItemQueryImpl lastModifiedDateBefore(Date lastModifiedDateBefore) {
        this.lastModifiedDateBefore = lastModifiedDateBefore;
        return this;
    }

    @Override
    public ContentItemQueryImpl lastModifiedDateAfter(Date lastModifiedDateAfter) {
        this.lastModifiedDateAfter = lastModifiedDateAfter;
        return this;
    }

    @Override
    public ContentItemQueryImpl lastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
        return this;
    }

    @Override
    public ContentItemQueryImpl lastModifiedByLike(String lastModifiedByLike) {
        this.lastModifiedByLike = lastModifiedByLike;
        return this;
    }

    @Override
    public ContentItemQueryImpl tenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("deploymentTenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public ContentItemQueryImpl tenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("deploymentTenantIdLike is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public ContentItemQueryImpl withoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    // sorting ////////////////////////////////////////////////////////

    @Override
    public ContentItemQuery orderByCreatedDate() {
        return orderBy(ContentItemQueryProperty.CREATED_DATE);
    }

    @Override
    public ContentItemQuery orderByTenantId() {
        return orderBy(ContentItemQueryProperty.TENANT_ID);
    }

    // results ////////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getContentItemEntityManager().findContentItemCountByQueryCriteria(this);
    }

    @Override
    public List<ContentItem> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getContentItemEntityManager().findContentItemsByQueryCriteria(this);
    }

    // getters ////////////////////////////////////////////////////////

    public String getId() {
        return id;
    }

    public Set<String> getIds() {
        return ids;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskIdLike() {
        return taskIdLike;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getProcessInstanceIdLike() {
        return processInstanceIdLike;
    }

    public String getScopeId() {
        return scopeId;
    }

    public String getScopeIdLike() {
        return scopeIdLike;
    }

    public String getScopeType() {
        return scopeType;
    }

    public String getScopeTypeLike() {
        return scopeTypeLike;
    }

    public String getContentStoreId() {
        return contentStoreId;
    }

    public String getContentStoreIdLike() {
        return contentStoreIdLike;
    }

    public String getContentStoreName() {
        return contentStoreName;
    }

    public String getContentStoreNameLike() {
        return contentStoreNameLike;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getMimeTypeLike() {
        return mimeTypeLike;
    }

    public String getField() {
        return field;
    }

    public String getFieldLike() {
        return fieldLike;
    }

    public Boolean getContentAvailable() {
        return contentAvailable;
    }

    public Long getContentSize() {
        return contentSize;
    }

    public Long getMinContentSize() {
        return minContentSize;
    }

    public Long getMaxContentSize() {
        return maxContentSize;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Date getCreatedDateBefore() {
        return createdDateBefore;
    }

    public Date getCreatedDateAfter() {
        return createdDateAfter;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getCreatedByLike() {
        return createdByLike;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Date getLastModifiedDateBefore() {
        return lastModifiedDateBefore;
    }

    public Date getLastModifiedDateAfter() {
        return lastModifiedDateAfter;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public String getLastModifiedByLike() {
        return lastModifiedByLike;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }
}
