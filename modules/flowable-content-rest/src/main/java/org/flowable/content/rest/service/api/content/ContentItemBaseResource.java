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

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentItemQuery;
import org.flowable.content.api.ContentService;
import org.flowable.content.engine.impl.ContentItemQueryProperty;
import org.flowable.content.rest.ContentRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Shared logic for resources related to Content items.
 * 
 * @author Frederik Heremans
 */
public class ContentItemBaseResource {

    private static HashMap<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("created", ContentItemQueryProperty.CREATED_DATE);
        properties.put("tenantId", ContentItemQueryProperty.TENANT_ID);
    }

    @Autowired
    protected ContentRestResponseFactory restResponseFactory;

    @Autowired
    protected ContentService contentService;

    protected DataResponse<ContentItemResponse> getContentItemsFromQueryRequest(ContentItemQueryRequest request, Map<String, String> requestParams) {

        ContentItemQuery contentItemQuery = contentService.createContentItemQuery();

        // Populate filter-parameters
        if (request.getId() != null) {
            contentItemQuery.id(request.getId());
        }

        if (request.getIds() != null) {
            contentItemQuery.ids(request.getIds());
        }

        if (request.getName() != null) {
            contentItemQuery.name(request.getName());
        }

        if (request.getNameLike() != null) {
            contentItemQuery.nameLike(request.getNameLike());
        }

        if (request.getMimeType() != null) {
            contentItemQuery.mimeType(request.getMimeType());
        }

        if (request.getMimeTypeLike() != null) {
            contentItemQuery.mimeTypeLike(request.getMimeTypeLike());
        }

        if (request.getTaskId() != null) {
            contentItemQuery.taskId(request.getTaskId());
        }

        if (request.getTaskIdLike() != null) {
            contentItemQuery.taskIdLike(request.getTaskIdLike());
        }

        if (request.getProcessInstanceId() != null) {
            contentItemQuery.processInstanceId(request.getProcessInstanceId());
        }

        if (request.getProcessInstanceIdLike() != null) {
            contentItemQuery.processInstanceIdLike(request.getProcessInstanceIdLike());
        }

        if (request.getContentStoreId() != null) {
            contentItemQuery.contentStoreId(request.getContentStoreId());
        }

        if (request.getContentStoreIdLike() != null) {
            contentItemQuery.contentStoreIdLike(request.getContentStoreIdLike());
        }

        if (request.getContentStoreName() != null) {
            contentItemQuery.contentStoreName(request.getContentStoreName());
        }

        if (request.getContentStoreNameLike() != null) {
            contentItemQuery.contentStoreNameLike(request.getContentStoreNameLike());
        }

        if (request.getContentSize() != null) {
            contentItemQuery.contentSize(request.getContentSize());
        }

        if (request.getMinimumContentSize() != null) {
            contentItemQuery.minContentSize(request.getMinimumContentSize());
        }

        if (request.getMaximumContentSize() != null) {
            contentItemQuery.maxContentSize(request.getMaximumContentSize());
        }

        if (request.getContentAvailable() != null) {
            contentItemQuery.contentAvailable(request.getContentAvailable());
        }

        if (request.getField() != null) {
            contentItemQuery.field(request.getField());
        }

        if (request.getFieldLike() != null) {
            contentItemQuery.fieldLike(request.getFieldLike());
        }

        if (request.getCreatedOn() != null) {
            contentItemQuery.createdDate(request.getCreatedOn());
        }

        if (request.getCreatedBefore() != null) {
            contentItemQuery.createdDateBefore(request.getCreatedBefore());
        }

        if (request.getCreatedAfter() != null) {
            contentItemQuery.createdDateAfter(request.getCreatedAfter());
        }

        if (request.getCreatedBy() != null) {
            contentItemQuery.createdBy(request.getCreatedBy());
        }

        if (request.getCreatedByLike() != null) {
            contentItemQuery.createdByLike(request.getCreatedByLike());
        }

        if (request.getLastModifiedOn() != null) {
            contentItemQuery.lastModifiedDate(request.getLastModifiedOn());
        }

        if (request.getLastModifiedBefore() != null) {
            contentItemQuery.lastModifiedDateBefore(request.getLastModifiedBefore());
        }

        if (request.getLastModifiedAfter() != null) {
            contentItemQuery.lastModifiedDateAfter(request.getLastModifiedAfter());
        }

        if (request.getLastModifiedBy() != null) {
            contentItemQuery.lastModifiedBy(request.getLastModifiedBy());
        }

        if (request.getLastModifiedByLike() != null) {
            contentItemQuery.lastModifiedByLike(request.getLastModifiedByLike());
        }

        if (request.getTenantId() != null) {
            contentItemQuery.tenantId(request.getTenantId());
        }

        if (request.getTenantIdLike() != null) {
            contentItemQuery.tenantIdLike(request.getTenantIdLike());
        }

        if (Boolean.TRUE.equals(request.getWithoutTenantId())) {
            contentItemQuery.withoutTenantId();
        }

        return new ContentItemPaginateList(restResponseFactory).paginateList(requestParams, request, contentItemQuery, "created", properties);
    }

    protected ContentItem getContentItemFromRequest(String contentItemId) {
        ContentItem contentItem = contentService.createContentItemQuery().id(contentItemId).singleResult();
        if (contentItem == null) {
            throw new FlowableObjectNotFoundException("Could not find a content item with id '" + contentItemId + "'.", ContentItem.class);
        }
        return contentItem;
    }
}
