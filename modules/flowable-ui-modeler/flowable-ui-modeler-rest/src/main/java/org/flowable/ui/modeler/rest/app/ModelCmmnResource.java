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

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Tijs Rademakers
 */
@RestController
@RequestMapping("/app")
public class ModelCmmnResource extends AbstractModelCmmnResource {

    /**
     * GET /rest/models/{modelId}/cmmn -> Get CMMN 1.1 xml
     */
    @RequestMapping(value = "/rest/models/{caseModelId}/cmmn", method = RequestMethod.GET)
    public void getProcessModelCmmnXml(HttpServletResponse response, @PathVariable String caseModelId) throws IOException {
        super.getCaseModelCmmnXml(response, caseModelId);
    }

    /**
     * GET /rest/models/{modelId}/history/{caseModelHistoryId}/cmmn -> Get CMMN 1.1 xml
     */
    @RequestMapping(value = "/rest/models/{caseModelId}/history/{caseModelHistoryId}/bpmn20", method = RequestMethod.GET)
    public void getHistoricProcessModelCmmnXml(HttpServletResponse response, @PathVariable String caseModelId, @PathVariable String caseModelHistoryId) throws IOException {
        super.getHistoricCaseModelCmmnXml(response, caseModelId, caseModelHistoryId);
    }

}
