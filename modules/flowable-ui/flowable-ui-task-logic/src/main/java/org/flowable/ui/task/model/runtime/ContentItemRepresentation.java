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
package org.flowable.ui.task.model.runtime;

import java.util.Date;

import org.flowable.content.api.ContentItem;
import org.flowable.ui.common.model.AbstractRepresentation;
import org.flowable.ui.task.model.component.SimpleContentTypeMapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author Tijs Rademakers
 */
public class ContentItemRepresentation extends AbstractRepresentation {

    protected String id;
    protected String name;
    protected boolean contentAvailable;
    protected String contentStoreId;
    protected String contentStoreName;
    protected String mimeType;
    protected String simpleType;
    protected Date created;
    protected String createdBy;

    public ContentItemRepresentation() {
    }

    public ContentItemRepresentation(ContentItem content, SimpleContentTypeMapper mapper) {
        this.id = content.getId();
        this.name = content.getName();
        this.contentStoreId = content.getContentStoreId();
        this.contentStoreName = content.getContentStoreName();
        this.created = content.getCreated();
        this.createdBy = content.getCreatedBy();
        this.contentAvailable = content.isContentAvailable();
        this.mimeType = content.getMimeType();

        if (mapper != null) {
            this.simpleType = mapper.getSimpleType(content);
        }
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

    public boolean isContentAvailable() {
        return contentAvailable;
    }

    public void setContentAvailable(boolean contentAvailable) {
        this.contentAvailable = contentAvailable;
    }

    @JsonInclude(Include.NON_NULL)
    public String getContentStoreId() {
        return contentStoreId;
    }

    public void setContentStoreId(String contentStoreId) {
        this.contentStoreId = contentStoreId;
    }

    @JsonInclude(Include.NON_NULL)
    public String getContentStoreName() {
        return contentStoreName;
    }

    public void setContentStoreName(String contentStoreName) {
        this.contentStoreName = contentStoreName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @JsonInclude(Include.NON_NULL)
    public String getSimpleType() {
        return simpleType;
    }

    public void setSimpleType(String simpleType) {
        this.simpleType = simpleType;
    }
}
