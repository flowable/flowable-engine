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
public interface ContentItemBaseQuery<T extends ContentItemBaseQuery<T, C>, C extends ContentItem> extends Query<T, C> {

    /**
     * Only select content items with the given id.
     */
    T id(String id);

    /**
     * Only select content items with the given ids.
     */
    T ids(Set<String> ids);

    /**
     * Only select content items with the given name.
     */
    T name(String name);

    /**
     * Only select content items with a name like the given string.
     */
    T nameLike(String nameLike);

    /**
     * Only select content items with the given mime type.
     */
    T mimeType(String mimeType);

    /**
     * Only select content items with a mime type like the given string.
     */
    T mimeTypeLike(String mimeTypeLike);

    /**
     * Only select content items with the given task id.
     */
    T taskId(String taskId);

    /**
     * Only select content items with a task id like the given string.
     */
    T taskIdLike(String taskIdLike);

    /**
     * Only select content items with the scope type as given string.
     */
    T scopeType(String scopeType);

    /**
     * Only select content items with the scope type like given string.
     */
    T scopeTypeLike(String scopeTypeLike);

    /**
     * Only select content items with the given scopeId.
     */
    T scopeId(String scopeId);

    /**
     * Only select content items with the scope id like given string.
     */
    T scopeIdLike(String scopeIdLike);

    /**
     * Only select content items with the given process instance id.
     */
    T processInstanceId(String processInstanceId);

    /**
     * Only select content items with a process instance id like the given string.
     */
    T processInstanceIdLike(String processInstanceIdLike);

    /**
     * Only select content items with the given content store id.
     */
    T contentStoreId(String contentStoreId);

    /**
     * Only select content items with a content store id like the given string.
     */
    T contentStoreIdLike(String contentStoreIdLike);

    /**
     * Only select content items with the given content store name.
     */
    T contentStoreName(String contentStoreName);

    /**
     * Only select content items with a content store name like the given string.
     */
    T contentStoreNameLike(String contentStoreNameLike);

    /**
     * Only select content items with content available or not.
     */
    T contentAvailable(Boolean contentAvailable);

    /**
     * Only select content items with the given content size.
     */
    T contentSize(Long contentSize);

    /**
     * Only select content items with the given minimal content size.
     */
    T minContentSize(Long minContentSize);

    /**
     * Only select content items with the given maximum content size.
     */
    T maxContentSize(Long maxContentSize);

    /**
     * Only select content items with the given field.
     */
    T field(String field);

    /**
     * Only select content items with a field like the given string.
     */
    T fieldLike(String fieldLike);

    /**
     * Only select content items created on the given time
     */
    T createdDate(Date submittedDate);

    /**
     * Only select content items created before the given time
     */
    T createdDateBefore(Date beforeTime);

    /**
     * Only select content items created after the given time
     */
    T createdDateAfter(Date afterTime);

    /**
     * Only select content items with the given created by value.
     */
    T createdBy(String submittedBy);

    /**
     * Only select content items with a create by like the given string.
     */
    T createdByLike(String submittedByLike);

    /**
     * Only select content items last modified on the given time
     */
    T lastModifiedDate(Date lastModifiedDate);

    /**
     * Only select content items last modified before the given time
     */
    T lastModifiedDateBefore(Date beforeTime);

    /**
     * Only select content items last modified after the given time
     */
    T lastModifiedDateAfter(Date afterTime);

    /**
     * Only select content items with the given last modified by value.
     */
    T lastModifiedBy(String lastModifiedBy);

    /**
     * Only select content items with a last modified by like the given string.
     */
    T lastModifiedByLike(String lastModifiedByLike);

    /**
     * Only select content items that have the given tenant id.
     */
    T tenantId(String tenantId);

    /**
     * Only select content items with a tenant id like the given one.
     */
    T tenantIdLike(String tenantIdLike);

    /**
     * Only select content items that do not have a tenant id.
     */
    T withoutTenantId();

    // sorting ////////////////////////////////////////////////////////

    /**
     * Order by created date (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByCreatedDate();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByTenantId();
}
