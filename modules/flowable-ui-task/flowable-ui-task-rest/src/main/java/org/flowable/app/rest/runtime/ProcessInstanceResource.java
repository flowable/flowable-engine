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
package org.flowable.app.rest.runtime;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.flowable.app.model.runtime.ProcessInstanceRepresentation;
import org.flowable.app.service.runtime.FlowableProcessInstanceService;
import org.flowable.form.model.FormModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

import static org.flowable.app.rest.HttpRequestHelper.executePostRequest;

/**
 * REST controller for managing a process instance.
 */
@RestController
public class ProcessInstanceResource {

    private static final Logger log = LoggerFactory.getLogger(ProcessInstanceResource.class);

    @Autowired
    protected FlowableProcessInstanceService processInstanceService;

    @Autowired
    protected Environment environment;

    @RequestMapping(value = "/rest/process-instances/{processInstanceId}", method = RequestMethod.GET, produces = "application/json")
    public ProcessInstanceRepresentation getProcessInstance(@PathVariable String processInstanceId, HttpServletResponse response) {
        return processInstanceService.getProcessInstance(processInstanceId, response);
    }

    @RequestMapping(value = "/rest/process-instances/{processInstanceId}/start-form", method = RequestMethod.GET, produces = "application/json")
    public FormModel getProcessInstanceStartForm(@PathVariable String processInstanceId, HttpServletResponse response) {
        return processInstanceService.getProcessInstanceStartForm(processInstanceId, response);
    }

    @RequestMapping(value = "/rest/process-instances/{processInstanceId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteProcessInstance(@PathVariable String processInstanceId) {
        processInstanceService.deleteProcessInstance(processInstanceId);
    }

    @RequestMapping(value = "/rest/models", method = RequestMethod.POST)
    public void createModel(@RequestParam String skeleton, @RequestBody String data) {
        String modelApiUrl = environment.getRequiredProperty("modeler.api.url");
        String basicAuthUser = environment.getRequiredProperty("idm.admin.user");
        String basicAuthPassword = environment.getRequiredProperty("idm.admin.password");

        if (!modelApiUrl.endsWith("/")) {
            modelApiUrl = modelApiUrl.concat("/");
        }
        modelApiUrl = modelApiUrl.concat("api/editor/models?skeleton=" + skeleton);

        executePostRequest(modelApiUrl, basicAuthUser, basicAuthPassword, new StringEntity(data, ContentType.create("application/json", "UTF-8")), org.apache.http.HttpStatus.SC_OK);
    }

}
