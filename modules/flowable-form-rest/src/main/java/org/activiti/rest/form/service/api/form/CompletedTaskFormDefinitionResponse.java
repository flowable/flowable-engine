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

import org.activiti.form.model.CompletedFormDefinition;
import org.activiti.form.model.FormDefinition;

/**
 * @author Yvo Swillens
 */
public class CompletedTaskFormDefinitionResponse extends CompletedFormDefinition {

  private String url;

  public CompletedTaskFormDefinitionResponse(FormDefinition formDefinition, String url) {
    super(formDefinition);
    this.url = url;
  }

  public CompletedTaskFormDefinitionResponse(CompletedFormDefinition completedFormDefinition) {

    super(completedFormDefinition);

    setSubmittedFormId(completedFormDefinition.getSubmittedFormId());
    setSubmittedBy(completedFormDefinition.getSubmittedBy());
    setSubmittedDate(completedFormDefinition.getSubmittedDate());
    setSelectedOutcome(completedFormDefinition.getSelectedOutcome());
    setTaskId(completedFormDefinition.getTaskId());
    setProcessInstanceId(completedFormDefinition.getProcessInstanceId());
    setProcessDefinitionId(completedFormDefinition.getProcessDefinitionId());
    setTenantId(completedFormDefinition.getTenantId());
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
