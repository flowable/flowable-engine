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
package org.flowable.dmn.engine.impl.audit;

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DecisionServiceExecutionAuditContainer;
import org.flowable.dmn.api.ExecuteDecisionContext;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DecisionTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class DecisionExecutionAuditUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecisionExecutionAuditUtil.class);

    public static DecisionServiceExecutionAuditContainer initializeDecisionServiceExecutionAudit(DecisionService decisionService, ExecuteDecisionContext executeDecisionInfo) {

        if (decisionService == null || decisionService.getId() == null) {
            LOGGER.error("decision service does not contain key");
            throw new IllegalArgumentException("decision does not contain decision key");
        }

        return new DecisionServiceExecutionAuditContainer(decisionService.getId(), decisionService.getName(), executeDecisionInfo.getDecisionVersion(),
           CommandContextUtil.getDmnEngineConfiguration().isStrictMode(), executeDecisionInfo.getVariables());
    }

    public static DecisionExecutionAuditContainer initializeDecisionExecutionAudit(Decision decision, ExecuteDecisionContext executeDecisionInfo) {

        if (decision == null || decision.getId() == null) {
            LOGGER.error("decision does not contain key");
            throw new IllegalArgumentException("decision does not contain decision key");
        }

        DecisionTable decisionTable = (DecisionTable) decision.getExpression();

        if (decisionTable.getHitPolicy() == null) {
            LOGGER.error("decision table does not contain a hit policy");
            throw new IllegalArgumentException("decision table does not contain a hit policy");
        }

        return new DecisionExecutionAuditContainer(decision.getId(), decision.getName(), executeDecisionInfo.getDecisionVersion(), 
                        decisionTable.getHitPolicy(), CommandContextUtil.getDmnEngineConfiguration().isStrictMode(), executeDecisionInfo.getVariables());
    }
}
