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
package org.activiti.content.engine.impl.cmd;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.activiti.content.api.ContentItem;
import org.activiti.content.api.ContentMetaDataKeys;
import org.activiti.content.api.ContentObject;
import org.activiti.content.api.ContentStorage;
import org.activiti.content.engine.impl.interceptor.Command;
import org.activiti.content.engine.impl.interceptor.CommandContext;
import org.activiti.content.engine.impl.persistence.entity.ContentItemEntity;
import org.activiti.engine.ActivitiIllegalArgumentException;

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

  public Void execute(CommandContext commandContext) {
    if (contentItem == null) {
      throw new ActivitiIllegalArgumentException("contentItem is null");
    }
    
    if (contentItem instanceof ContentItemEntity == false) {
      throw new ActivitiIllegalArgumentException("contentItem is not of type ContentItemEntity");
    }
    
    ContentItemEntity contentItemEntity = (ContentItemEntity) contentItem;
    
    if (inputStream != null) {
      // Stream given, write to store and save a reference to the content object
      Map<String, Object> metaData = new HashMap<String, Object>();
      if (contentItem.getTaskId() != null) {
        metaData.put(ContentMetaDataKeys.TASK_ID, contentItem.getTaskId());
      } else {
        if (contentItem.getProcessInstanceId() != null) {
          metaData.put(ContentMetaDataKeys.PROCESS_INSTANCE_ID, contentItem.getProcessInstanceId());
        }
      }
      
      ContentStorage contentStorage = commandContext.getContentEngineConfiguration().getContentStorage();
      ContentObject createContentObject = contentStorage.createContentObject(inputStream, metaData);
      contentItemEntity.setContentStoreId(createContentObject.getId());
      contentItemEntity.setContentStoreName(contentStorage.getContentStoreName());
      contentItemEntity.setContentAvailable(true);

      // After storing the stream, store the length to be accessible without having to consult the
      // underlying content storage to get file size
      contentItemEntity.setContentSize(createContentObject.getContentLength());
    }
    
    if (contentItemEntity.getLastModified() == null) {
      contentItemEntity.setLastModified(new Date());
    }

    if (contentItem.getId() == null) {
      if (contentItemEntity.getCreated() == null) {
        contentItemEntity.setCreated(new Date());
      }
      
      commandContext.getContentItemEntityManager().insert(contentItemEntity);
      
    } else {
      commandContext.getContentItemEntityManager().update(contentItemEntity);
    }
    
    return null;
  }

}
