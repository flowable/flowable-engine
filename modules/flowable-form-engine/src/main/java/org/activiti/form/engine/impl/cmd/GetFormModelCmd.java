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

import java.io.Serializable;

import org.activiti.editor.form.converter.FormJsonConverter;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.form.engine.FormEngineConfiguration;
import org.activiti.form.engine.impl.interceptor.Command;
import org.activiti.form.engine.impl.interceptor.CommandContext;
import org.activiti.form.engine.impl.persistence.deploy.DeploymentManager;
import org.activiti.form.engine.impl.persistence.deploy.FormDefinitionCacheEntry;
import org.activiti.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.activiti.form.model.FormModel;

/**
 * @author Tijs Rademakers
 */
public class GetFormModelCmd implements Command<FormModel>, Serializable {

  private static final long serialVersionUID = 1L;

  protected String formDefinitionKey;
  protected String formDefinitionId;
  protected String tenantId;
  protected String parentDeploymentId;

  public GetFormModelCmd(String formDefinitionKey, String formDefinitionId) {
    this.formDefinitionKey = formDefinitionKey;
    this.formDefinitionId = formDefinitionId;
  }
  
  public GetFormModelCmd(String formDefinitionKey, String formDefinitionId, String tenantId) {
    this(formDefinitionKey, formDefinitionId);
    this.tenantId = tenantId;
  }
  
  public GetFormModelCmd(String formDefinitionKey, String formDefinitionId, String tenantId, String parentDeploymentId) {
    this(formDefinitionKey, formDefinitionId, tenantId);
    this.parentDeploymentId = parentDeploymentId;
  }

  public FormModel execute(CommandContext commandContext) {
    DeploymentManager deploymentManager = commandContext.getFormEngineConfiguration().getDeploymentManager();

    // Find the form definition
    FormDefinitionEntity formDefinitionEntity = null;
    if (formDefinitionId != null) {

      formDefinitionEntity = deploymentManager.findDeployedFormDefinitionById(formDefinitionId);
      if (formDefinitionEntity == null) {
        throw new ActivitiObjectNotFoundException("No form definition found for id = '" + formDefinitionId + "'", FormDefinitionEntity.class);
      }

    } else if (formDefinitionKey != null && (tenantId == null || FormEngineConfiguration.NO_TENANT_ID.equals(tenantId)) && parentDeploymentId == null) {

      formDefinitionEntity = deploymentManager.findDeployedLatestFormDefinitionByKey(formDefinitionKey);
      if (formDefinitionEntity == null) {
        throw new ActivitiObjectNotFoundException("No form definition found for key '" + formDefinitionKey + "'", FormDefinitionEntity.class);
      }

    } else if (formDefinitionKey != null && tenantId != null && !FormEngineConfiguration.NO_TENANT_ID.equals(tenantId) && parentDeploymentId == null) {

      formDefinitionEntity = deploymentManager.findDeployedLatestFormDefinitionByKeyAndTenantId(formDefinitionKey, tenantId);
      if (formDefinitionEntity == null) {
        throw new ActivitiObjectNotFoundException("No form definition found for key '" + formDefinitionKey + "' for tenant identifier " + tenantId, FormDefinitionEntity.class);
      }
      
    } else if (formDefinitionKey != null && (tenantId == null || FormEngineConfiguration.NO_TENANT_ID.equals(tenantId)) && parentDeploymentId != null) {

      formDefinitionEntity = deploymentManager.findDeployedLatestFormDefinitionByKeyAndParentDeploymentId(formDefinitionKey, parentDeploymentId);
      if (formDefinitionEntity == null) {
        throw new ActivitiObjectNotFoundException("No form definition found for key '" + formDefinitionKey + 
            "' for parent deployment id " + parentDeploymentId, FormDefinitionEntity.class);
      }
      
    } else if (formDefinitionKey != null && tenantId != null && !FormEngineConfiguration.NO_TENANT_ID.equals(tenantId) && parentDeploymentId != null) {

      formDefinitionEntity = deploymentManager.findDeployedLatestFormDefinitionByKeyParentDeploymentIdAndTenantId(formDefinitionKey, parentDeploymentId, tenantId);
      if (formDefinitionEntity == null) {
        throw new ActivitiObjectNotFoundException("No form definition found for key '" + formDefinitionKey + 
            "for parent deployment id '" + parentDeploymentId + "' and for tenant identifier " + tenantId, FormDefinitionEntity.class);
      }

    } else {
      throw new ActivitiObjectNotFoundException("formDefinitionKey and formDefinitionId are null");
    }
    
    FormDefinitionCacheEntry formDefinitionCacheEntry = deploymentManager.resolveFormDefinition(formDefinitionEntity);
    FormJsonConverter formJsonConverter = commandContext.getFormEngineConfiguration().getFormJsonConverter();
    return formJsonConverter.convertToFormModel(formDefinitionCacheEntry.getFormDefinitionJson(), 
        formDefinitionEntity.getId(), formDefinitionEntity.getVersion());
  }
}