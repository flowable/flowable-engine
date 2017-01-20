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
package org.activiti.form.engine.impl.deployer;

import org.activiti.editor.form.converter.FormJsonConverter;
import org.activiti.form.engine.FormEngineConfiguration;
import org.activiti.form.engine.impl.context.Context;
import org.activiti.form.engine.impl.persistence.deploy.DeploymentCache;
import org.activiti.form.engine.impl.persistence.deploy.FormDefinitionCacheEntry;
import org.activiti.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.activiti.form.engine.impl.persistence.entity.FormDeploymentEntity;
import org.activiti.form.model.FormModel;

/**
 * Updates caches and artifacts for a deployment and its forms
 */
public class CachingAndArtifactsManager {
  
  protected FormJsonConverter formJsonConverter = new FormJsonConverter();
  
  /**
   * Ensures that the decision table is cached in the appropriate places, including the
   * deployment's collection of deployed artifacts and the deployment manager's cache.
   */
  public void updateCachingAndArtifacts(ParsedDeployment parsedDeployment) {
    final FormEngineConfiguration formEngineConfiguration = Context.getFormEngineConfiguration();
    DeploymentCache<FormDefinitionCacheEntry> formDefinitionCache = formEngineConfiguration.getDeploymentManager().getFormCache();
    FormDeploymentEntity deployment = parsedDeployment.getDeployment();

    for (FormDefinitionEntity formDefinition : parsedDeployment.getAllFormDefinitions()) {
      FormModel formModel = parsedDeployment.getFormModelForFormDefinition(formDefinition);
      formModel.setId(formDefinition.getId());
      FormDefinitionCacheEntry cacheEntry = new FormDefinitionCacheEntry(formDefinition, formJsonConverter.convertToJson(formModel));
      formDefinitionCache.add(formDefinition.getId(), cacheEntry);
    
      // Add to deployment for further usage
      deployment.addDeployedArtifact(formDefinition);
    }
  }
}

