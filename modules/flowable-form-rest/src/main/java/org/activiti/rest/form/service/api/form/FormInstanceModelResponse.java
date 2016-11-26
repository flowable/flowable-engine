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

import org.activiti.form.model.FormInstanceModel;
import org.activiti.form.model.FormModel;

/**
 * @author Yvo Swillens
 */
public class FormInstanceModelResponse extends FormInstanceModel {

  private String url;

  public FormInstanceModelResponse(FormModel formModel, String url) {
    super(formModel);
    this.url = url;
  }

  public FormInstanceModelResponse(FormInstanceModel formInstanceModel) {

    super(formInstanceModel);

    setFormInstanceId(formInstanceModel.getFormInstanceId());
    setSubmittedBy(formInstanceModel.getSubmittedBy());
    setSubmittedDate(formInstanceModel.getSubmittedDate());
    setSelectedOutcome(formInstanceModel.getSelectedOutcome());
    setTaskId(formInstanceModel.getTaskId());
    setProcessInstanceId(formInstanceModel.getProcessInstanceId());
    setProcessDefinitionId(formInstanceModel.getProcessDefinitionId());
    setTenantId(formInstanceModel.getTenantId());
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
