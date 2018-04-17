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
package org.flowable.ui.modeler.rest.app;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.flowable.ui.modeler.model.FormSaveRepresentation;
import org.flowable.ui.modeler.model.form.FormRepresentation;
import org.flowable.ui.modeler.service.FlowableFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Tijs Rademakers
 */
@RestController
@RequestMapping("/app/rest/form-models")
public class FormResource {

    @Autowired
    protected FlowableFormService formService;

    @RequestMapping(value = "/{formId}", method = RequestMethod.GET, produces = "application/json")
    public FormRepresentation getForm(@PathVariable String formId) {
        return formService.getForm(formId);
    }

    @RequestMapping(value = "/values", method = RequestMethod.GET, produces = "application/json")
    public List<FormRepresentation> getForms(HttpServletRequest request) {
        String[] formIds = request.getParameterValues("formId");
        return formService.getForms(formIds);
    }

    @RequestMapping(value = "/{formId}/history/{formHistoryId}", method = RequestMethod.GET, produces = "application/json")
    public FormRepresentation getFormHistory(@PathVariable String formId, @PathVariable String formHistoryId) {
        return formService.getFormHistory(formId, formHistoryId);
    }

    @RequestMapping(value = "/{formId}", method = RequestMethod.PUT, produces = "application/json")
    public FormRepresentation saveForm(@PathVariable String formId, @RequestBody FormSaveRepresentation saveRepresentation) {
        return formService.saveForm(formId, saveRepresentation);
    }
}
