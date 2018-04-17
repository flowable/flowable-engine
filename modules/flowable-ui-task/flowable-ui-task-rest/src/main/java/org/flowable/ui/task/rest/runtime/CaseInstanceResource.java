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

import javax.servlet.http.HttpServletResponse;

import org.flowable.form.model.SimpleFormModel;
import org.flowable.ui.task.model.runtime.CaseInstanceRepresentation;
import org.flowable.ui.task.service.runtime.FlowableCaseInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing a case instance.
 */
@RestController
@RequestMapping("/app")
public class CaseInstanceResource {

    @Autowired
    protected FlowableCaseInstanceService caseInstanceService;

    @RequestMapping(value = "/rest/case-instances/{caseInstanceId}", method = RequestMethod.GET, produces = "application/json")
    public CaseInstanceRepresentation getCaseInstance(@PathVariable String caseInstanceId) {
        return caseInstanceService.getCaseInstance(caseInstanceId);
    }

    @RequestMapping(value = "/rest/case-instances/{caseInstanceId}/start-form", method = RequestMethod.GET, produces = "application/json")
    public SimpleFormModel getCaseInstanceStartForm(@PathVariable String caseInstanceId, HttpServletResponse response) {
        //return caseInstanceService.getProcessInstanceStartForm(caseInstanceId, response);
        return null;
    }

    @RequestMapping(value = "/rest/case-instances/{caseInstanceId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteCaseInstance(@PathVariable String caseInstanceId) {
        caseInstanceService.deleteCaseInstance(caseInstanceId);
    }

}
