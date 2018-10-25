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

import org.flowable.form.api.FormInfo;
import org.flowable.form.model.SimpleFormModel;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.task.model.runtime.CaseInstanceRepresentation;
import org.flowable.ui.task.model.runtime.CreateCaseInstanceRepresentation;
import org.flowable.ui.task.model.runtime.FormModelRepresentation;
import org.flowable.ui.task.service.runtime.FlowableCaseInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
    public FormModelRepresentation getCaseInstanceStartForm(@PathVariable String caseInstanceId, HttpServletResponse response) {
        FormInfo formInfo = caseInstanceService.getCaseInstanceStartForm(caseInstanceId);
        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        return new FormModelRepresentation(formInfo, formModel);
    }

    @RequestMapping(value = "/rest/case-instances/{caseInstanceId}/active-stages", method = RequestMethod.GET, produces = "application/json")
    public ResultListDataRepresentation getCaseInstanceActiveStages(@PathVariable String caseInstanceId) {
        return caseInstanceService.getCaseInstanceActiveStages(caseInstanceId);
    }

    @RequestMapping(value = "/rest/case-instances/{caseInstanceId}/ended-stages", method = RequestMethod.GET, produces = "application/json")
    public ResultListDataRepresentation getCaseInstanceEndedStages(@PathVariable String caseInstanceId) {
        return caseInstanceService.getCaseInstanceEndedStages(caseInstanceId);
    }

    @RequestMapping(value = "/rest/case-instances/{caseInstanceId}/available-milestones", method = RequestMethod.GET, produces = "application/json")
    public ResultListDataRepresentation getCaseInstanceAvailableMilestones(@PathVariable String caseInstanceId) {
        return caseInstanceService.getCaseInstanceAvailableMilestones(caseInstanceId);
    }

    @RequestMapping(value = "/rest/case-instances/{caseInstanceId}/ended-milestones", method = RequestMethod.GET, produces = "application/json")
    public ResultListDataRepresentation getCaseInstanceEndedMilestones(@PathVariable String caseInstanceId) {
        return caseInstanceService.getCaseInstanceEndedMilestones(caseInstanceId);
    }

    @RequestMapping(value = "/rest/case-instances/{caseInstanceId}/available-user-event-listeners", method = RequestMethod.GET, produces = "application/json")
    public ResultListDataRepresentation getCaseInstanceAvailableUserEventListeners(@PathVariable String caseInstanceId) {
        return caseInstanceService.getCaseInstanceAvailableUserEventListeners(caseInstanceId);
    }

    @RequestMapping(value = "/rest/case-instances/{caseInstanceId}/completed-user-event-listeners", method = RequestMethod.GET, produces = "application/json")
    public ResultListDataRepresentation getCaseInstanceCompletedUserEventListeners(@PathVariable String caseInstanceId) {
        return caseInstanceService.getCaseInstanceCompletedUserEventListeners(caseInstanceId);
    }

    @RequestMapping(value = "/rest/case-instances/{caseInstanceId}/trigger-user-event-listener/{userEventListenerId}", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void triggerUserEventListener(@PathVariable String caseInstanceId, @PathVariable String userEventListenerId) {
        caseInstanceService.triggerUserEventListener(caseInstanceId, userEventListenerId);
    }

    @RequestMapping(value = "/rest/case-instances/{caseInstanceId}/enabled-planitem-instances", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public ResultListDataRepresentation getCaseInstanceEnabledPlanItemInstances(@PathVariable String caseInstanceId) {
        return caseInstanceService.getCaseInstanceEnabledPlanItemInstances(caseInstanceId);
    }

    @RequestMapping(value = "/rest/case-instances/{caseInstanceId}/enabled-planitem-instances/{planItemInstanceId}", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void startEnabledPlanItemInstance(@PathVariable String caseInstanceId, @PathVariable String planItemInstanceId) {
        caseInstanceService.startEnabledPlanItemInstance(caseInstanceId, planItemInstanceId);
    }

    @RequestMapping(value = "/rest/case-instances/{caseInstanceId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteCaseInstance(@PathVariable String caseInstanceId) {
        caseInstanceService.deleteCaseInstance(caseInstanceId);
    }

}
