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

package org.flowable.content.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;
import org.flowable.content.api.ContentItem;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.impl.ContentItemQueryImpl;
import org.flowable.content.engine.impl.persistence.entity.data.ContentItemDataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ContentItemEntityManagerImpl
    extends AbstractEngineEntityManager<ContentEngineConfiguration, ContentItemEntity, ContentItemDataManager>
    implements ContentItemEntityManager {

    public ContentItemEntityManagerImpl(ContentEngineConfiguration contentEngineConfiguration, ContentItemDataManager contentItemDataManager) {
        super(contentEngineConfiguration, contentItemDataManager);
    }

    @Override
    public long findContentItemCountByQueryCriteria(ContentItemQueryImpl contentItemQuery) {
        return dataManager.findContentItemCountByQueryCriteria(contentItemQuery);
    }

    @Override
    public List<ContentItem> findContentItemsByQueryCriteria(ContentItemQueryImpl contentItemQuery) {
        return dataManager.findContentItemsByQueryCriteria(contentItemQuery);
    }

    @Override
    public void deleteContentItemsByTaskId(String taskId) {
        dataManager.deleteContentItemsByTaskId(taskId);
    }

    @Override
    public void deleteContentItemsByProcessInstanceId(String processInstanceId) {
        dataManager.deleteContentItemsByProcessInstanceId(processInstanceId);
    }

    @Override
    public void deleteContentItemsByScopeIdAndScopeType(String scopeId, String scopeType) {
        dataManager.deleteContentItemsByScopeIdAndScopeType(scopeId, scopeType);
    }

}
