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

import java.io.Serializable;

import org.activiti.content.api.ContentStorage;
import org.activiti.content.engine.impl.interceptor.Command;
import org.activiti.content.engine.impl.interceptor.CommandContext;
import org.activiti.content.engine.impl.persistence.entity.ContentItemEntity;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;

/**
 * @author Tijs Rademakers
 */
public class DeleteContentItemCmd implements Command<Void>, Serializable {

  private static final long serialVersionUID = 1L;
  
  protected String contentItemId;

  public DeleteContentItemCmd(String contentItemId) {
    this.contentItemId = contentItemId;
  }
  
  public Void execute(CommandContext commandContext) {
    if (contentItemId == null) {
      throw new ActivitiIllegalArgumentException("contentItemId is null");
    }
    
    ContentItemEntity contentItem = (ContentItemEntity) commandContext.getContentItemEntityManager().findById(contentItemId);
    if (contentItem == null) {
      throw new ActivitiObjectNotFoundException("content item could not be found with id " + contentItemId);
    }
    
    if (contentItem.getContentStoreId() != null) {
      ContentStorage contentStorage = commandContext.getContentEngineConfiguration().getContentStorage();
      contentStorage.deleteContentObject(contentItem.getContentStoreId());
    }
    
    commandContext.getContentItemEntityManager().delete(contentItem);
    
    return null;
  }

}
