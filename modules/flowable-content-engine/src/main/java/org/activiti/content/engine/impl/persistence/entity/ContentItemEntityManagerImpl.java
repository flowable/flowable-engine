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

package org.activiti.content.engine.impl.persistence.entity;

import java.util.List;

import org.activiti.content.api.ContentItem;
import org.activiti.content.engine.ContentEngineConfiguration;
import org.activiti.content.engine.impl.ContentItemQueryImpl;
import org.activiti.content.engine.impl.persistence.entity.data.ContentItemDataManager;
import org.activiti.engine.common.impl.Page;
import org.activiti.engine.common.impl.persistence.entity.data.DataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ContentItemEntityManagerImpl extends AbstractEntityManager<ContentItemEntity> implements ContentItemEntityManager {

  protected ContentItemDataManager contentItemDataManager;
  
  public ContentItemEntityManagerImpl(ContentEngineConfiguration contentEngineConfiguration, ContentItemDataManager contentItemDataManager) {
    super(contentEngineConfiguration);
    this.contentItemDataManager = contentItemDataManager;
  }
  
  @Override
  public long findContentItemCountByQueryCriteria(ContentItemQueryImpl contentItemQuery) {
    return contentItemDataManager.findContentItemCountByQueryCriteria(contentItemQuery);
  }

  @Override
  public List<ContentItem> findContentItemsByQueryCriteria(ContentItemQueryImpl contentItemQuery, Page page) {
    return contentItemDataManager.findContentItemsByQueryCriteria(contentItemQuery, page);
  }
  
  @Override
  public void deleteContentItemsByTaskId(String taskId) {
    contentItemDataManager.deleteContentItemsByTaskId(taskId);
  }

  @Override
  public void deleteContentItemsByProcessInstanceId(String processInstanceId) {
    contentItemDataManager.deleteContentItemsByProcessInstanceId(processInstanceId);
  }

  @Override
  protected DataManager<ContentItemEntity> getDataManager() {
    return contentItemDataManager;
  }

  public ContentItemDataManager getContentItemDataManager() {
    return contentItemDataManager;
  }

  public void setContentItemDataManager(ContentItemDataManager contentItemDataManager) {
    this.contentItemDataManager = contentItemDataManager;
  }
  
}
