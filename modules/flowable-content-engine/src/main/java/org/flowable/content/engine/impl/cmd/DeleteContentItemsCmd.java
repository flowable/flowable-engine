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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.content.api.ContentItem;
import org.flowable.content.engine.impl.persistence.entity.ContentItemEntityManager;
import org.flowable.content.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DeleteContentItemsCmd extends AbstractDeleteContentItemCmd implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String processInstanceId;
    protected String taskId;
    protected String scopeId;
    protected String scopeType;

    public DeleteContentItemsCmd(String processInstanceId, String taskId, String scopeId, String scopeType) {
        this.processInstanceId = processInstanceId;
        this.taskId = taskId;
        this.scopeId = scopeId;
        this.scopeType = scopeType;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (processInstanceId == null && taskId == null && scopeId == null) {
            throw new FlowableIllegalArgumentException("taskId, processInstanceId and scopeId are null");
        }

        ContentItemEntityManager contentItemEntityManager = CommandContextUtil.getContentItemEntityManager();
        if (processInstanceId != null) {

            List<ContentItem> contentItems = contentItemEntityManager.findContentItemsByProcessInstanceId(processInstanceId);
            if (contentItems != null) {
                for (ContentItem contentItem : contentItems) {
                    deleteContentItemInContentStorage(contentItem);
                }
            }

            contentItemEntityManager.deleteContentItemsByProcessInstanceId(processInstanceId);

        } else if (StringUtils.isNotEmpty(scopeId)) {

            List<ContentItem> contentItems = contentItemEntityManager.findContentItemsByScopeIdAndScopeType(scopeId, scopeType);
            if (contentItems != null) {
                for (ContentItem contentItem : contentItems) {
                    deleteContentItemInContentStorage(contentItem);
                }
            }

            contentItemEntityManager.deleteContentItemsByScopeIdAndScopeType(scopeId, scopeType);

        } else {

            List<ContentItem> contentItems = contentItemEntityManager.findContentItemsByTaskId(taskId);
            if (contentItems != null) {
                for (ContentItem contentItem : contentItems) {
                    deleteContentItemInContentStorage(contentItem);
                }
            }

            contentItemEntityManager.deleteContentItemsByTaskId(taskId);
        }

        return null;
    }

}
