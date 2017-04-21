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
package org.flowable.dmn.engine.impl.cmd;

import org.apache.commons.lang3.StringUtils;
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.RuleEngineExecutionResult;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.interceptor.Command;
import org.flowable.dmn.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.engine.impl.persistence.deploy.DecisionTableCacheEntry;
import org.flowable.dmn.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public abstract class AbstractExecuteDecisionCmd implements Serializable{

    private static final long serialVersionUID = 1L;

    protected String decisionKey;
    protected String parentDeploymentId;
    protected Map<String, Object> variables;
    protected String tenantId;

    protected DmnDecisionTable resolveDecisionTable(DeploymentManager deploymentManager) {
        DmnDecisionTable decisionTable = null;

        if (StringUtils.isNotEmpty(decisionKey) && StringUtils.isNotEmpty(parentDeploymentId) && StringUtils.isNotEmpty(tenantId)) {
            decisionTable = deploymentManager.findDeployedLatestDecisionByKeyParentDeploymentIdAndTenantId(decisionKey, parentDeploymentId, tenantId);
            if (decisionTable == null) {
                throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey +
                    ", parent deployment id " + parentDeploymentId + " and tenant id: " + tenantId);
            }

        } else if (StringUtils.isNotEmpty(decisionKey) && StringUtils.isNotEmpty(parentDeploymentId)) {
            decisionTable = deploymentManager.findDeployedLatestDecisionByKeyAndParentDeploymentId(decisionKey, parentDeploymentId);
            if (decisionTable == null) {
                throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey +
                    " and parent deployment id " + parentDeploymentId);
            }

        } else if (StringUtils.isNotEmpty(decisionKey) && StringUtils.isNotEmpty(tenantId)) {
            decisionTable = deploymentManager.findDeployedLatestDecisionByKeyAndTenantId(decisionKey, tenantId);
            if (decisionTable == null) {
                throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey +
                    " and tenant id " + tenantId);
            }

        } else if (StringUtils.isNotEmpty(decisionKey)) {
            decisionTable = deploymentManager.findDeployedLatestDecisionByKey(decisionKey);
            if (decisionTable == null) {
                throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey);
            }

        } else {
            throw new IllegalArgumentException("decisionKey is null");
        }

        return decisionTable;
    }

    protected Decision resolveDecision(DeploymentManager deploymentManager, DmnDecisionTable decisionTable) {
        if (decisionTable == null) {
            throw new IllegalArgumentException("decisionTable is null");
        }

        DecisionTableCacheEntry decisionTableCacheEntry = deploymentManager.resolveDecisionTable(decisionTable);
        Decision decision = decisionTableCacheEntry.getDecision();

        return decision;
    }
}
