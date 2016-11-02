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
package org.activiti.rest.dmn.service.api.management;

import org.activiti.dmn.engine.DmnEngine;
import org.activiti.dmn.engine.DmnEngineInfo;
import org.activiti.dmn.engine.DmnEngines;
import org.activiti.engine.ActivitiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yvo Swillens
 */
@RestController
public class DmnEngineResource {

    @Autowired
    protected DmnEngine dmnEngine;

    @RequestMapping(value = "/dmn-management/engine", method = RequestMethod.GET, produces = "application/json")
    public DmnEngineInfoResponse getEngineInfo() {
        DmnEngineInfoResponse response = new DmnEngineInfoResponse();

        try {
            DmnEngineInfo dmnEngineInfo = DmnEngines.getDmnEngineInfo(dmnEngine.getName());
            if (dmnEngineInfo != null) {
                response.setName(dmnEngineInfo.getName());
                response.setResourceUrl(dmnEngineInfo.getResourceUrl());
                response.setException(dmnEngineInfo.getException());
            } else {
                response.setName(dmnEngine.getName());
            }
        } catch (Exception e) {
            throw new ActivitiException("Error retrieving DMN engine info", e);
        }

        response.setVersion(DmnEngine.VERSION);

        return response;
    }

}
