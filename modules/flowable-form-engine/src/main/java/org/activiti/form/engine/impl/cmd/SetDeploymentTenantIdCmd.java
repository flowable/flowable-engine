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
import java.util.List;

import org.activiti.engine.common.api.ActivitiIllegalArgumentException;
import org.activiti.engine.common.api.ActivitiObjectNotFoundException;
import org.activiti.form.api.FormDefinition;
import org.activiti.form.engine.impl.FormDefinitionQueryImpl;
import org.activiti.form.engine.impl.interceptor.Command;
import org.activiti.form.engine.impl.interceptor.CommandContext;
import org.activiti.form.engine.impl.persistence.entity.FormDeploymentEntity;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SetDeploymentTenantIdCmd implements Command<Void>, Serializable {

  private static final long serialVersionUID = 1L;

  protected String deploymentId;
  protected String newTenantId;

  public SetDeploymentTenantIdCmd(String deploymentId, String newTenantId) {
    this.deploymentId = deploymentId;
    this.newTenantId = newTenantId;
  }

  public Void execute(CommandContext commandContext) {
    if (deploymentId == null) {
      throw new ActivitiIllegalArgumentException("deploymentId is null");
    }

    // Update all entities

    FormDeploymentEntity deployment = commandContext.getDeploymentEntityManager().findById(deploymentId);
    if (deployment == null) {
      throw new ActivitiObjectNotFoundException("Could not find deployment with id " + deploymentId);
    }
    
    deployment.setTenantId(newTenantId);

    commandContext.getFormDefinitionEntityManager().updateFormDefinitionTenantIdForDeployment(deploymentId, newTenantId);

    // Doing decision tables in memory, cause we need to clear the decision table cache
    List<FormDefinition> formDefinitions = new FormDefinitionQueryImpl().deploymentId(deploymentId).list();
    for (FormDefinition formDefinition : formDefinitions) {
      commandContext.getFormEngineConfiguration().getFormDefinitionCache().remove(formDefinition.getId());
    }
    
    commandContext.getDeploymentEntityManager().update(deployment);

    return null;

  }

}
