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
package org.activiti.content.engine.impl;

import java.io.InputStream;

import org.activiti.content.api.ContentItem;
import org.activiti.content.api.ContentItemQuery;
import org.activiti.content.api.ContentService;
import org.activiti.content.engine.impl.cmd.CreateContentItemCmd;
import org.activiti.content.engine.impl.cmd.DeleteContentItemCmd;
import org.activiti.content.engine.impl.cmd.DeleteContentItemsCmd;
import org.activiti.content.engine.impl.cmd.GetContentItemStreamCmd;
import org.activiti.content.engine.impl.cmd.SaveContentItemCmd;

/**
 * @author Tijs Rademakers
 */
public class ContentServiceImpl extends ServiceImpl implements ContentService {

  public ContentItem newContentItem() {
    return commandExecutor.execute(new CreateContentItemCmd());
  }
  
  public void saveContentItem(ContentItem contentItem) {
    commandExecutor.execute(new SaveContentItemCmd(contentItem));
  }
  
  public void saveContentItem(ContentItem contentItem, InputStream inputStream) {
    commandExecutor.execute(new SaveContentItemCmd(contentItem, inputStream));
  }
  
  public InputStream getContentItemData(String contentItemId) {
    return commandExecutor.execute(new GetContentItemStreamCmd(contentItemId));
  }
  
  public void deleteContentItem(String contentItemId) {
    commandExecutor.execute(new DeleteContentItemCmd(contentItemId));
  }
  
  public void deleteContentItemsByProcessInstanceId(String processInstanceId) {
    commandExecutor.execute(new DeleteContentItemsCmd(processInstanceId, null));
  }

  public void deleteContentItemsByTaskId(String taskId) {
    commandExecutor.execute(new DeleteContentItemsCmd(null, taskId));
  }
  
  public ContentItemQuery createContentItemQuery() {
    return new ContentItemQueryImpl(commandExecutor);
  }
}
