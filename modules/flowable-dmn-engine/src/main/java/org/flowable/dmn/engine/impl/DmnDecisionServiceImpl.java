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
package org.flowable.dmn.engine.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.ExecuteDecisionBuilder;
import org.flowable.dmn.api.ExecuteDecisionContext;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.cmd.ExecuteDecisionCmd;
import org.flowable.dmn.engine.impl.cmd.ExecuteDecisionWithAuditTrailCmd;
import org.flowable.dmn.model.DecisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class DmnDecisionServiceImpl extends CommonEngineServiceImpl<DmnEngineConfiguration> implements DmnDecisionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DmnDecisionServiceImpl.class);

    @Override
    public ExecuteDecisionBuilder createExecuteDecisionBuilder() {
        return new ExecuteDecisionBuilderImpl(this);
    }

    public List<Map<String, Object>> executeDecision(ExecuteDecisionBuilder executeDecisionBuilder) {
        ExecuteDecisionContext executeDecisionContext = execute(executeDecisionBuilder);

        List<Map<String, Object>> decisionResult = composeDecisionResult(executeDecisionContext);

        return decisionResult;
    }
    
    public Map<String, Object> executeDecisionWithSingleResult(ExecuteDecisionBuilder executeDecisionBuilder) {
        ExecuteDecisionContext executeDecisionContext = execute(executeDecisionBuilder);

        Map<String, Object> singleDecisionResult = null;
        List<Map<String, Object>> decisionResult = composeDecisionResult(executeDecisionContext);

        if (decisionResult != null && !decisionResult.isEmpty()) {
            if (decisionResult.size() > 1) {
                throw new FlowableException("more than one result");
            }
            singleDecisionResult = decisionResult.get(0);
        }

        return singleDecisionResult;
    }

    public DecisionExecutionAuditContainer executeDecisionWithAuditTrail(ExecuteDecisionBuilder executeDecisionBuilder) {
        ExecuteDecisionContext executeDecisionContext = executeDecisionBuilder.buildExecuteDecisionContext();

        commandExecutor.execute(new ExecuteDecisionWithAuditTrailCmd(executeDecisionContext));

        DecisionExecutionAuditContainer decisionExecution = executeDecisionContext.getDecisionExecution();

        decisionExecution.stopAudit();

        List<Map<String, Object>> decisionResult = composeDecisionResult(executeDecisionContext);
        decisionExecution.setDecisionResult(decisionResult);

        return decisionExecution;
    }

    protected ExecuteDecisionContext execute(ExecuteDecisionBuilder executeDecisionBuilder) {
        ExecuteDecisionContext executeDecisionContext = executeDecisionBuilder.buildExecuteDecisionContext();

        commandExecutor.execute(new ExecuteDecisionCmd(executeDecisionContext));

        executeDecisionContext.getDecisionExecution().stopAudit();

        return executeDecisionContext;
    }

    protected List<Map<String, Object>> composeDecisionResult(ExecuteDecisionContext executeDecisionContext) {
        // check if execution was Decision or DecisionService
        if (executeDecisionContext.getDmnElement() instanceof DecisionService) {
            List<Map<String, Object>> result = new ArrayList<>();
            DecisionService decisionService = (DecisionService) executeDecisionContext.getDmnElement();
            decisionService.getOutputDecisions().forEach(elementReference ->
                result.addAll(executeDecisionContext.getDecisionExecution().getChildDecisionExecution(elementReference.getParsedId()).getDecisionResult()));
            return result;
        } else {
            return executeDecisionContext.getDecisionExecution().getDecisionResult();
        }
    }
}