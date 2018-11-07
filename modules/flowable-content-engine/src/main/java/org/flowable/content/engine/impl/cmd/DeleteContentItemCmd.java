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

import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.content.api.ContentStorage;
import org.flowable.content.engine.impl.persistence.entity.ContentItemEntity;
import org.flowable.content.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class DeleteContentItemCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String contentItemId;

    public DeleteContentItemCmd(String contentItemId) {
        this.contentItemId = contentItemId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (contentItemId == null) {
            throw new FlowableIllegalArgumentException("contentItemId is null");
        }

        ContentItemEntity contentItem = (ContentItemEntity) CommandContextUtil.getContentItemEntityManager().findById(contentItemId);
        if (contentItem == null) {
            throw new FlowableObjectNotFoundException("content item could not be found with id " + contentItemId);
        }

        if (contentItem.getContentStoreId() != null) {
            ContentStorage contentStorage = CommandContextUtil.getContentEngineConfiguration().getContentStorage();
            if (contentItem.isContentAvailable()) {
                contentStorage.deleteContentObject(contentItem.getContentStoreId());
            }
        }

        CommandContextUtil.getContentItemEntityManager().delete(contentItem);

        return null;
    }

}
