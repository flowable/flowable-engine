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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DecisionServiceExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.ExecuteDecisionBuilder;
import org.flowable.dmn.api.ExecuteDecisionContext;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.cmd.EvaluateDecisionCmd;
import org.flowable.dmn.engine.impl.cmd.ExecuteDecisionCmd;
import org.flowable.dmn.engine.impl.cmd.ExecuteDecisionServiceCmd;
import org.flowable.dmn.engine.impl.cmd.ExecuteDecisionWithAuditTrailCmd;
import org.flowable.dmn.engine.impl.cmd.PersistHistoricDecisionExecutionCmd;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DmnElementReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 * @author Valentin Zickner
 */
public class DmnDecisionServiceImpl extends CommonEngineServiceImpl<DmnEngineConfiguration> implements DmnDecisionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DmnDecisionServiceImpl.class);

    public DmnDecisionServiceImpl(DmnEngineConfiguration engineConfiguration) {
        super(engineConfiguration);
    }

    @Override
    public ExecuteDecisionBuilder createExecuteDecisionBuilder() {
        return new ExecuteDecisionBuilderImpl(this);
    }

    @Override
    public List<Map<String, Object>> executeDecision(ExecuteDecisionBuilder builder) {
        ExecuteDecisionContext executeDecisionContext = builder.buildExecuteDecisionContext();

        commandExecutor.execute(new ExecuteDecisionCmd(executeDecisionContext));

        List<Map<String, Object>> decisionResult = composeDecisionResult(executeDecisionContext);

        finalizeDecisionExecutionAudit(executeDecisionContext);

        return decisionResult;
    }

    @Override
    public Map<String, List<Map<String, Object>>> executeDecisionService(ExecuteDecisionBuilder builder) {
        ExecuteDecisionContext executeDecisionContext = builder.buildExecuteDecisionContext();

        commandExecutor.execute(new ExecuteDecisionServiceCmd(executeDecisionContext));

        Map<String, List<Map<String, Object>>> decisionResult = composeDecisionServiceResult(executeDecisionContext);

        finalizeDecisionExecutionAudit(executeDecisionContext);

        return decisionResult;
    }

    @Override
    public Map<String, Object> executeWithSingleResult(ExecuteDecisionBuilder builder) {
        ExecuteDecisionContext executeDecisionContext = builder.buildExecuteDecisionContext();

        commandExecutor.execute(new EvaluateDecisionCmd(executeDecisionContext));

        Map<String, Object> singleDecisionResult = null;
        Map<String, List<Map<String, Object>>> decisionResult = composeEvaluateDecisionResult(executeDecisionContext);

        finalizeDecisionExecutionAudit(executeDecisionContext);

        for (Map.Entry<String, List<Map<String, Object>>> entry : decisionResult.entrySet()) {
            List<Map<String, Object>> decisionResults = entry.getValue();
            if (decisionResults != null && !decisionResults.isEmpty()) {
                if (decisionResults.size() > 1) {
                    throw new FlowableException("more than one result in decision: " + entry.getKey());
                }
                if (singleDecisionResult == null) {
                    singleDecisionResult = new HashMap<>();
                }
                singleDecisionResult.putAll(decisionResults.get(0));
            }
        }

        return singleDecisionResult;
    }

    @Override
    public Map<String, Object> executeDecisionWithSingleResult(ExecuteDecisionBuilder builder) {
        ExecuteDecisionContext executeDecisionContext = builder.buildExecuteDecisionContext();

        commandExecutor.execute(new ExecuteDecisionCmd(executeDecisionContext));

        Map<String, Object> singleDecisionResult = null;
        List<Map<String, Object>> decisionResult = composeDecisionResult(executeDecisionContext);

        finalizeDecisionExecutionAudit(executeDecisionContext);

        if (decisionResult != null && !decisionResult.isEmpty()) {
            if (decisionResult.size() > 1) {
                throw new FlowableException("more than one result");
            }
            singleDecisionResult = decisionResult.get(0);
        }

        return singleDecisionResult;
    }

    @Override
    public Map<String, Object> executeDecisionServiceWithSingleResult(ExecuteDecisionBuilder builder) {
        ExecuteDecisionContext executeDecisionContext = builder.buildExecuteDecisionContext();

        commandExecutor.execute(new ExecuteDecisionServiceCmd(executeDecisionContext));

        Map<String, Object> singleDecisionResult = new HashMap<>();
        Map<String, List<Map<String, Object>>> decisionResult = composeDecisionServiceResult(executeDecisionContext);

        finalizeDecisionExecutionAudit(executeDecisionContext);

        for (Map.Entry<String, List<Map<String, Object>>> entry : decisionResult.entrySet()) {
            List<Map<String, Object>> decisionResults = entry.getValue();
            if (decisionResults != null && !decisionResults.isEmpty()) {
                if (decisionResults.size() > 1) {
                    throw new FlowableException("more than one result in decision: " + entry.getKey());
                }
                singleDecisionResult.putAll(decisionResults.get(0));
            }
        }

        return singleDecisionResult;
    }

    @Override
    public DecisionExecutionAuditContainer executeWithAuditTrail(ExecuteDecisionBuilder builder) {
        ExecuteDecisionContext executeDecisionContext = builder.buildExecuteDecisionContext();

        commandExecutor.execute(new EvaluateDecisionCmd(executeDecisionContext));

        composeEvaluateDecisionResult(executeDecisionContext);

        DecisionExecutionAuditContainer decisionExecution = finalizeDecisionExecutionAudit(executeDecisionContext);

        return decisionExecution;
    }

    @Override
    public DecisionExecutionAuditContainer executeDecisionWithAuditTrail(ExecuteDecisionBuilder builder) {
        ExecuteDecisionContext executeDecisionContext = builder.buildExecuteDecisionContext();

        commandExecutor.execute(new ExecuteDecisionWithAuditTrailCmd(executeDecisionContext));

        composeDecisionResult(executeDecisionContext);

        DecisionExecutionAuditContainer decisionExecution = finalizeDecisionExecutionAudit(executeDecisionContext);

        return decisionExecution;
    }

    @Override
    public DecisionServiceExecutionAuditContainer executeDecisionServiceWithAuditTrail(ExecuteDecisionBuilder builder) {
        ExecuteDecisionContext executeDecisionContext = builder.buildExecuteDecisionContext();

        commandExecutor.execute(new ExecuteDecisionServiceCmd(executeDecisionContext));

        composeDecisionServiceResult(executeDecisionContext);

        DecisionServiceExecutionAuditContainer decisionServiceExecutionAuditContainer = (DecisionServiceExecutionAuditContainer) finalizeDecisionExecutionAudit(executeDecisionContext);

        return decisionServiceExecutionAuditContainer;
    }



    protected Map<String, List<Map<String, Object>>> composeEvaluateDecisionResult(ExecuteDecisionContext executeDecisionContext) {
        Map<String, List<Map<String, Object>>> result;

        // check if execution was Decision or DecisionService
        if (executeDecisionContext.getDmnElement() instanceof DecisionService) {
            result = composeDecisionServiceResult(executeDecisionContext);
        } else if (executeDecisionContext.getDmnElement() instanceof Decision) {
            result = new HashMap<>();
            result.put(executeDecisionContext.getDecisionKey(), executeDecisionContext.getDecisionExecution().getDecisionResult());
        } else {
            LOGGER.error("Execution was not a decision or decision service");
            throw new FlowableException("Execution was not a decision or decision service");
        }

        return result;
    }

    protected List<Map<String, Object>> composeDecisionResult(ExecuteDecisionContext executeDecisionContext) {
        return executeDecisionContext.getDecisionExecution().getDecisionResult();
    }

    protected Map<String, List<Map<String, Object>>> composeDecisionServiceResult(ExecuteDecisionContext executeDecisionContext) {
        // check if execution was Decision or DecisionService
        if (executeDecisionContext.getDmnElement() instanceof DecisionService decisionService) {
            Map<String, List<Map<String, Object>>> decisionServiceResult = new HashMap<>();
            DecisionServiceExecutionAuditContainer decisionServiceExecutionAuditContainer = (DecisionServiceExecutionAuditContainer) executeDecisionContext.getDecisionExecution();

            boolean multipleResults = decisionService.getOutputDecisions().size() > 1;
            for (DmnElementReference elementReference : decisionService.getOutputDecisions()) {
                DecisionExecutionAuditContainer childDecisionExecution = decisionServiceExecutionAuditContainer
                        .getChildDecisionExecution(elementReference.getParsedId());
                if (childDecisionExecution.getDecisionResult() != null && !childDecisionExecution.getDecisionResult().isEmpty()) {
                    decisionServiceResult.put(elementReference.getParsedId(), childDecisionExecution.getDecisionResult());
                }
                multipleResults = multipleResults || childDecisionExecution.isMultipleResults();
            }

            decisionServiceExecutionAuditContainer.setDecisionServiceResult(decisionServiceResult);
            decisionServiceExecutionAuditContainer.setMultipleResults(multipleResults);
            return decisionServiceResult;
        } else {
            throw new FlowableException("Main execution was a not a decision service");
        }
    }

    protected DecisionExecutionAuditContainer finalizeDecisionExecutionAudit(ExecuteDecisionContext executeDecisionContext) {
        DecisionExecutionAuditContainer decisionExecution = executeDecisionContext.getDecisionExecution();

        decisionExecution.stopAudit(configuration.getClock().getCurrentTime());

        if (!executeDecisionContext.isDisableHistory()) {
            commandExecutor.execute(new PersistHistoricDecisionExecutionCmd(executeDecisionContext));
        }

        return decisionExecution;
    }
}
