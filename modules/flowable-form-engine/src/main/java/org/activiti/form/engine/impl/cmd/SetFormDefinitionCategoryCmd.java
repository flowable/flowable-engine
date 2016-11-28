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
package org.activiti.form.engine.impl.cmd;

import org.activiti.engine.common.api.ActivitiIllegalArgumentException;
import org.activiti.engine.common.api.ActivitiObjectNotFoundException;
import org.activiti.form.engine.impl.interceptor.Command;
import org.activiti.form.engine.impl.interceptor.CommandContext;
import org.activiti.form.engine.impl.persistence.deploy.DeploymentCache;
import org.activiti.form.engine.impl.persistence.deploy.FormDefinitionCacheEntry;
import org.activiti.form.engine.impl.persistence.entity.FormDefinitionEntity;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SetFormDefinitionCategoryCmd implements Command<Void> {

  protected String formDefinitionId;
  protected String category;

  public SetFormDefinitionCategoryCmd(String formDefinitionId, String category) {
    this.formDefinitionId = formDefinitionId;
    this.category = category;
  }

  public Void execute(CommandContext commandContext) {

    if (formDefinitionId == null) {
      throw new ActivitiIllegalArgumentException("Form definition id is null");
    }

    FormDefinitionEntity formDefinition = commandContext.getFormDefinitionEntityManager().findById(formDefinitionId);

    if (formDefinition == null) {
      throw new ActivitiObjectNotFoundException("No form definition found for id = '" + formDefinitionId + "'");
    }

    // Update category
    formDefinition.setCategory(category);

    // Remove form from cache, it will be refetched later
    DeploymentCache<FormDefinitionCacheEntry> formDefinitionCache = commandContext.getFormEngineConfiguration().getFormDefinitionCache();
    if (formDefinitionCache != null) {
      formDefinitionCache.remove(formDefinitionId);
    }
    
    commandContext.getFormDefinitionEntityManager().update(formDefinition);

    return null;
  }

  public String getFormDefinitionId() {
    return formDefinitionId;
  }

  public void setFormDefinitionId(String formDefinitionId) {
    this.formDefinitionId = formDefinitionId;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

}
