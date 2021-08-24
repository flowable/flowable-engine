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
package org.flowable.content.engine.impl.cmd;

import java.io.InputStream;
import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentObject;
import org.flowable.content.api.ContentObjectStorageMetadata;
import org.flowable.content.api.ContentStorage;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.impl.persistence.entity.ContentItemEntity;
import org.flowable.content.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class SaveContentItemCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected ContentItem contentItem;
    protected InputStream inputStream;

    public SaveContentItemCmd(ContentItem contentItem) {
        this.contentItem = contentItem;
    }

    public SaveContentItemCmd(ContentItem contentItem, InputStream inputStream) {
        this.contentItem = contentItem;
        this.inputStream = inputStream;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (contentItem == null) {
            throw new FlowableIllegalArgumentException("contentItem is null");
        }

        if (!(contentItem instanceof ContentItemEntity)) {
            throw new FlowableIllegalArgumentException("contentItem is not of type ContentItemEntity");
        }

        ContentItemEntity contentItemEntity = (ContentItemEntity) contentItem;

        ContentEngineConfiguration contentEngineConfiguration = CommandContextUtil.getContentEngineConfiguration();

        if (inputStream != null) {
            // Stream given, write to store and save a reference to the content object

            ContentStorage contentStorage = contentEngineConfiguration.getContentStorage();
            
            ContentObject contentObject;
            if (StringUtils.isNotEmpty(contentItemEntity.getContentStoreId())) {
                contentObject = contentStorage.updateContentObject(contentItemEntity.getContentStoreId(), inputStream, new ContentItemContentObjectMetadata());
            } else {
                contentObject = contentStorage.createContentObject(inputStream, new ContentItemContentObjectMetadata());
                contentItemEntity.setContentStoreId(contentObject.getId());
            }
            
            contentItemEntity.setContentStoreName(contentStorage.getContentStoreName());
            contentItemEntity.setContentAvailable(true);

            // After storing the stream, store the length to be accessible without having to consult the
            // underlying content storage to get file size
            contentItemEntity.setContentSize(contentObject.getContentLength());

            // Make lastModified timestamp update whenever the content changes
            contentItemEntity.setLastModified(contentEngineConfiguration.getClock().getCurrentTime());
        }

        if (contentItemEntity.getLastModified() == null) {
            contentItemEntity.setLastModified(contentEngineConfiguration.getClock().getCurrentTime());
        }

        if (contentItem.getId() == null) {
            if (contentItemEntity.getCreated() == null) {
                contentItemEntity.setCreated(contentEngineConfiguration.getClock().getCurrentTime());
            }
            contentEngineConfiguration.getContentItemEntityManager().insert(contentItemEntity);
            
        } else {
            contentEngineConfiguration.getContentItemEntityManager().update(contentItemEntity);
        }

        return null;
    }

    protected class ContentItemContentObjectMetadata implements ContentObjectStorageMetadata {

        @Override
        public String getName() {
            return contentItem.getName();
        }

        @Override
        public String getScopeId() {
            if (contentItem.getTaskId() != null) {
                return contentItem.getTaskId();
            } else if (contentItem.getProcessInstanceId() != null) {
                return contentItem.getProcessInstanceId();
            } else {
                return contentItem.getScopeId();
            }
        }

        @Override
        public String getScopeType() {
            if (contentItem.getTaskId() != null) {
                return ScopeTypes.TASK;
            } else if (contentItem.getProcessInstanceId() != null) {
                return ScopeTypes.BPMN;
            } else {
                return contentItem.getScopeType();
            }
        }

        @Override
        public String getMimeType() {
            return contentItem.getMimeType();
        }

        @Override
        public String getTenantId() {
            return contentItem.getTenantId();
        }

        @Override
        public Object getStoredObject() {
            return contentItem;
        }
    }

}
