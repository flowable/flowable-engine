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
package org.activiti.rest.form.service.api.management;

import org.activiti.engine.ActivitiException;
import org.activiti.form.engine.FormEngine;
import org.activiti.form.engine.FormEngineInfo;
import org.activiti.form.engine.FormEngines;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yvo Swillens
 */
@RestController
public class FormEngineResource {

    @Autowired
    protected FormEngine formEngine;

    @RequestMapping(value = "/form-management/engine", method = RequestMethod.GET, produces = "application/json")
    public FormEngineInfoResponse getEngineInfo() {
        FormEngineInfoResponse response = new FormEngineInfoResponse();

        try {
            FormEngineInfo formEngineInfo = FormEngines.getFormEngineInfo(formEngine.getName());
            if (formEngineInfo != null) {
                response.setName(formEngineInfo.getName());
                response.setResourceUrl(formEngineInfo.getResourceUrl());
                response.setException(formEngineInfo.getException());
            } else {
                response.setName(formEngine.getName());
            }
        } catch (Exception e) {
            throw new ActivitiException("Error retrieving form engine info", e);
        }

        response.setVersion(FormEngine.VERSION);

        return response;
    }
}
