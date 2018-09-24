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

package org.flowable.content.api;

import java.util.Date;
import java.util.Set;

import org.flowable.common.engine.api.query.Query;

/**
 * Allows programmatic querying of {@link ContentItem}s.
 *
 * @author Tijs Rademakers
 */
public interface ContentItemQuery extends Query<ContentItemQuery, ContentItem> {

    /**
     * Only select content items with the given id.
     */
    ContentItemQuery id(String id);

    /**
     * Only select content items with the given ids.
     */
    ContentItemQuery ids(Set<String> ids);

    /**
     * Only select content items with the given name.
     */
    ContentItemQuery name(String name);

    /**
     * Only select content items with a name like the given string.
     */
    ContentItemQuery nameLike(String nameLike);

    /**
     * Only select content items with the given mime type.
     */
    ContentItemQuery mimeType(String mimeType);

    /**
     * Only select content items with a mime type like the given string.
     */
    ContentItemQuery mimeTypeLike(String mimeTypeLike);

    /**
     * Only select content items with the given task id.
     */
    ContentItemQuery taskId(String taskId);

    /**
     * Only select content items with a task id like the given string.
     */
    ContentItemQuery taskIdLike(String taskIdLike);

    /**
     * Only select content items with the scope type as given string.
     */
    ContentItemQuery scopeType(String scopeType);

    /**
     * Only select content items with the scope type like given string.
     */
    ContentItemQuery scopeTypeLike(String scopeTypeLike);

    /**
     * Only select content items with the given scopeId.
     */
    ContentItemQuery scopeId(String scopeId);

    /**
     * Only select content items with the scope id like given string.
     */
    ContentItemQuery scopeIdLike(String scopeIdLike);

    /**
     * Only select content items with the given process instance id.
     */
    ContentItemQuery processInstanceId(String processInstanceId);

    /**
     * Only select content items with a process instance id like the given string.
     */
    ContentItemQuery processInstanceIdLike(String processInstanceIdLike);

    /**
     * Only select content items with the given content store id.
     */
    ContentItemQuery contentStoreId(String contentStoreId);

    /**
     * Only select content items with a content store id like the given string.
     */
    ContentItemQuery contentStoreIdLike(String contentStoreIdLike);

    /**
     * Only select content items with the given content store name.
     */
    ContentItemQuery contentStoreName(String contentStoreName);

    /**
     * Only select content items with a content store name like the given string.
     */
    ContentItemQuery contentStoreNameLike(String contentStoreNameLike);

    /**
     * Only select content items with content available or not.
     */
    ContentItemQuery contentAvailable(Boolean contentAvailable);

    /**
     * Only select content items with the given content size.
     */
    ContentItemQuery contentSize(Long contentSize);

    /**
     * Only select content items with the given minimal content size.
     */
    ContentItemQuery minContentSize(Long minContentSize);

    /**
     * Only select content items with the given maximum content size.
     */
    ContentItemQuery maxContentSize(Long maxContentSize);

    /**
     * Only select content items with the given field.
     */
    ContentItemQuery field(String field);

    /**
     * Only select content items with a field like the given string.
     */
    ContentItemQuery fieldLike(String fieldLike);

    /**
     * Only select content items created on the given time
     */
    ContentItemQuery createdDate(Date submittedDate);

    /**
     * Only select content items created before the given time
     */
    ContentItemQuery createdDateBefore(Date beforeTime);

    /**
     * Only select content items created after the given time
     */
    ContentItemQuery createdDateAfter(Date afterTime);

    /**
     * Only select content items with the given created by value.
     */
    ContentItemQuery createdBy(String submittedBy);

    /**
     * Only select content items with a create by like the given string.
     */
    ContentItemQuery createdByLike(String submittedByLike);

    /**
     * Only select content items last modified on the given time
     */
    ContentItemQuery lastModifiedDate(Date lastModifiedDate);

    /**
     * Only select content items last modified before the given time
     */
    ContentItemQuery lastModifiedDateBefore(Date beforeTime);

    /**
     * Only select content items last modified after the given time
     */
    ContentItemQuery lastModifiedDateAfter(Date afterTime);

    /**
     * Only select content items with the given last modified by value.
     */
    ContentItemQuery lastModifiedBy(String lastModifiedBy);

    /**
     * Only select content items with a last modified by like the given string.
     */
    ContentItemQuery lastModifiedByLike(String lastModifiedByLike);

    /**
     * Only select content items that have the given tenant id.
     */
    ContentItemQuery tenantId(String tenantId);

    /**
     * Only select content items with a tenant id like the given one.
     */
    ContentItemQuery tenantIdLike(String tenantIdLike);

    /**
     * Only select content items that do not have a tenant id.
     */
    ContentItemQuery withoutTenantId();

    // sorting ////////////////////////////////////////////////////////

    /**
     * Order by created date (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ContentItemQuery orderByCreatedDate();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ContentItemQuery orderByTenantId();
}
