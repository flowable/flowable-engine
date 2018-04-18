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
package org.flowable.ui.task.rest.runtime;

import org.flowable.form.api.FormInfo;
import org.flowable.form.model.SimpleFormModel;
import org.flowable.ui.task.model.runtime.FormModelRepresentation;
import org.flowable.ui.task.service.runtime.FlowableProcessDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app")
public class ProcessDefinitionResource {

    @Autowired
    protected FlowableProcessDefinitionService processDefinitionService;

    @RequestMapping(value = "/rest/process-definitions/{processDefinitionId}/start-form", method = RequestMethod.GET, produces = "application/json")
    public FormModelRepresentation getProcessDefinitionStartForm(@PathVariable String processDefinitionId) {
        FormInfo formInfo = processDefinitionService.getProcessDefinitionStartForm(processDefinitionId);
        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        return new FormModelRepresentation(formInfo, formModel);
    }
}
