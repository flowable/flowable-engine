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
package org.activiti.rest.form.service.api.form;

import org.activiti.form.model.FormDefinition;

/**
 * @author Yvo Swillens
 */
public class TaskFormDefinitionResponse extends FormDefinition {

  private String url;

  public TaskFormDefinitionResponse(FormDefinition formDefinition) {
    setId(formDefinition.getId());
    setName(formDefinition.getName());
    setDescription(formDefinition.getDescription());
    setKey(formDefinition.getKey());
    setVersion(formDefinition.getVersion());
    setFields(formDefinition.getFields());
    setOutcomes(formDefinition.getOutcomes());
    setOutcomeVariableName(formDefinition.getOutcomeVariableName());
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
