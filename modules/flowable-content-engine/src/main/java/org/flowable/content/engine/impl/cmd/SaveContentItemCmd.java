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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentMetaDataKeys;
import org.flowable.content.api.ContentObject;
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
            Map<String, Object> metaData = new HashMap<>();
            if (contentItem.getTaskId() != null) {
                metaData.put(ContentMetaDataKeys.TASK_ID, contentItem.getTaskId());
            } else {
                if (contentItem.getProcessInstanceId() != null) {
                    metaData.put(ContentMetaDataKeys.PROCESS_INSTANCE_ID, contentItem.getProcessInstanceId());
                } else {
                    if (StringUtils.isNotEmpty(contentItem.getScopeType())) {
                        metaData.put(ContentMetaDataKeys.SCOPE_TYPE, contentItem.getScopeType());
                    }
                    if (StringUtils.isNotEmpty(contentItem.getScopeId())) {
                        metaData.put(ContentMetaDataKeys.SCOPE_ID, contentItem.getScopeId());
                    }
                }
            }

            ContentStorage contentStorage = contentEngineConfiguration.getContentStorage();
            ContentObject createContentObject = contentStorage.createContentObject(inputStream, metaData);
            contentItemEntity.setContentStoreId(createContentObject.getId());
            contentItemEntity.setContentStoreName(contentStorage.getContentStoreName());
            contentItemEntity.setContentAvailable(true);

            // After storing the stream, store the length to be accessible without having to consult the
            // underlying content storage to get file size
            contentItemEntity.setContentSize(createContentObject.getContentLength());
        }

        if (contentItemEntity.getLastModified() == null) {
            contentItemEntity.setLastModified(contentEngineConfiguration.getClock().getCurrentTime());
        }

        if (contentItem.getId() == null) {
            if (contentItemEntity.getCreated() == null) {
                contentItemEntity.setCreated(contentEngineConfiguration.getClock().getCurrentTime());
            }

            CommandContextUtil.getContentItemEntityManager().insert(contentItemEntity);

        } else {
            CommandContextUtil.getContentItemEntityManager().update(contentItemEntity);
        }

        return null;
    }

}
