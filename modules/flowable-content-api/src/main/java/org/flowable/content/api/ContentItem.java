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

/**
 * An object structure representing a piece of content.
 *
 * @author Tijs Rademakers
 * @author Joram Barez
 */
public interface ContentItem {

    /** unique identifier */
    String getId();

    String getName();

    void setName(String name);

    String getMimeType();

    void setMimeType(String mimeType);

    String getTaskId();

    void setTaskId(String taskId);

    String getProcessInstanceId();

    void setProcessInstanceId(String processInstanceId);

    String getScopeId();

    void setScopeId(String scopeId);

    String getScopeType();

    void setScopeType(String scopeType);

    String getContentStoreId();

    void setContentStoreId(String contentStoreId);

    String getContentStoreName();

    void setContentStoreName(String contentStoreName);

    boolean isContentAvailable();

    String getField();

    void setField(String field);

    Long getContentSize();

    String getTenantId();

    void setTenantId(String tenantId);

    Date getCreated();

    String getCreatedBy();

    void setCreatedBy(String createdBy);

    Date getLastModified();

    String getLastModifiedBy();

    void setLastModifiedBy(String lastModifiedBy);

    boolean isProvisional();

    void setProvisional(boolean provisional);
}
