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
package org.flowable.form.engine.impl.deployer;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.flowable.form.engine.impl.context.Context;
import org.flowable.form.engine.impl.parser.FormDefinitionParse;
import org.flowable.form.engine.impl.parser.FormDefinitionParseFactory;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntity;
import org.flowable.form.engine.impl.persistence.entity.ResourceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParsedDeploymentBuilder {

  private static final Logger log = LoggerFactory.getLogger(ParsedDeploymentBuilder.class);
  
  public static final String[] FORM_RESOURCE_SUFFIXES = new String[] { "form" };

  protected FormDeploymentEntity deployment;
  protected FormDefinitionParseFactory formDefinitionParseFactory;

  public ParsedDeploymentBuilder(FormDeploymentEntity deployment, FormDefinitionParseFactory formDefinitionParseFactory) {
    this.deployment = deployment;
    this.formDefinitionParseFactory = formDefinitionParseFactory;
  }

  public ParsedDeployment build() {
    List<FormDefinitionEntity> formDefinitions = new ArrayList<FormDefinitionEntity>();
    Map<FormDefinitionEntity, FormDefinitionParse> formDefinitionToParseMap = new LinkedHashMap<FormDefinitionEntity, FormDefinitionParse>();
    Map<FormDefinitionEntity, ResourceEntity> formDefintionToResourceMap = new LinkedHashMap<FormDefinitionEntity, ResourceEntity>();

    for (ResourceEntity resource : deployment.getResources().values()) {
      if (isFormResource(resource.getName())) {
        log.debug("Processing Form definition resource {}", resource.getName());
        FormDefinitionParse parse = createFormParseFromResource(resource);
        for (FormDefinitionEntity formDefinition : parse.getFormDefinitions()) {
          formDefinitions.add(formDefinition);
          formDefinitionToParseMap.put(formDefinition, parse);
          formDefintionToResourceMap.put(formDefinition, resource);
        }
      }
    }

    return new ParsedDeployment(deployment, formDefinitions, formDefinitionToParseMap, formDefintionToResourceMap);
  }

  protected FormDefinitionParse createFormParseFromResource(ResourceEntity resource) {
    String resourceName = resource.getName();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(resource.getBytes());

    FormDefinitionParse formParse = formDefinitionParseFactory.createParse()
        .sourceInputStream(inputStream)
        .setSourceSystemId(resourceName)
        .deployment(deployment)
        .name(resourceName);
    
    formParse.execute(Context.getFormEngineConfiguration());
    return formParse;
  }

  protected boolean isFormResource(String resourceName) {
    for (String suffix : FORM_RESOURCE_SUFFIXES) {
      if (resourceName.endsWith(suffix)) {
        return true;
      }
    }

    return false;
  }

}