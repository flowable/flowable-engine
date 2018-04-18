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
package org.flowable.dmn.rest.service.api.history;

import javax.servlet.http.HttpServletResponse;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.rest.resolver.ContentTypeResolver;
import org.flowable.dmn.api.DmnHistoricDecisionExecution;
import org.flowable.dmn.api.DmnHistoryService;
import org.flowable.dmn.rest.service.api.DmnRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Tijs Rademakers
 */
@RestController
public class BaseHistoricDecisionExecutionResource {

    @Autowired
    protected ContentTypeResolver contentTypeResolver;

    @Autowired
    protected DmnRestResponseFactory dmnRestResponseFactory;

    @Autowired
    protected DmnHistoryService dmnHistoryService;

    /**
     * Returns the {@link DmnHistoricDecisionExecution} that is requested. Throws the right exceptions when bad request was made or decision table is not found.
     */
    protected DmnHistoricDecisionExecution getHistoricDecisionExecutionFromRequest(String decisionExecutionId) {
        DmnHistoricDecisionExecution decisionExecution = dmnHistoryService.createHistoricDecisionExecutionQuery().id(decisionExecutionId).singleResult();

        if (decisionExecution == null) {
            throw new FlowableObjectNotFoundException("Could not find a decision execution with id '" + decisionExecution);
        }
        return decisionExecution;
    }

    protected String getExecutionAuditData(String decisionExecutionId, HttpServletResponse response) {

        if (decisionExecutionId == null) {
            throw new FlowableIllegalArgumentException("No decision execution id provided");
        }

        // Check if deployment exists
        DmnHistoricDecisionExecution decisionExecution = getHistoricDecisionExecutionFromRequest(decisionExecutionId);
        response.setContentType("application/json");
        return decisionExecution.getExecutionJson();
    }
}
