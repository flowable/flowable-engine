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
package org.flowable.dmn.engine.impl.deployer;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntity;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;

/**
 * Methods for working with deployments. Much of the actual work of {@link DmnDeployer} is done by orchestrating the different pieces of work this class does; by having them here, we allow other
 * deployers to make use of them.
 */
public class DmnDeploymentHelper {

    /**
     * Verifies that no two decision tables share the same key, to prevent database unique index violation.
     * 
     * @throws FlowableException
     *             if any two decision tables have the same key
     */
    public void verifyDecisionTablesDoNotShareKeys(Collection<DecisionEntity> decisionTables) {
        Set<String> keySet = new LinkedHashSet<>();
        for (DecisionEntity decisionTable : decisionTables) {
            if (keySet.contains(decisionTable.getKey())) {
                throw new FlowableException(
                        "The deployment contains decision tables with the same key (decision id attribute), this is not allowed");
            }
            keySet.add(decisionTable.getKey());
        }
    }

    /**
     * Updates all the decision table entities to match the deployment's values for tenant, engine version, and deployment id.
     */
    public void copyDeploymentValuesToDecisions(DmnDeploymentEntity deployment, List<DecisionEntity> decisions) {
        String tenantId = deployment.getTenantId();
        String deploymentId = deployment.getId();

        for (DecisionEntity decision : decisions) {
            // decision inherits the tenant id
            if (tenantId != null) {
                decision.setTenantId(tenantId);
            }
            decision.setDeploymentId(deploymentId);
        }
    }

    /**
     * Updates all the decision table entities to have the correct resource names.
     */
    public void setResourceNamesOnDecisions(ParsedDeployment parsedDeployment) {
        for (DecisionEntity decision : parsedDeployment.getAllDecisions()) {
            String resourceName = parsedDeployment.getResourceForDecision(decision).getName();
            decision.setResourceName(resourceName);
        }
    }

    /**
     * Gets the most recent persisted decision that matches this one for tenant and key. If none is found, returns null. This method assumes that the tenant and key are properly set on the
     * decision entity.
     */
    public DecisionEntity getMostRecentVersionOfDecision(DecisionEntity decision) {
        String key = decision.getKey();
        String tenantId = decision.getTenantId();
        DecisionEntityManager decisionTableEntityManager = CommandContextUtil.getDmnEngineConfiguration().getDecisionEntityManager();

        DecisionEntity existingDecision;

        if (tenantId != null && !tenantId.equals(DmnEngineConfiguration.NO_TENANT_ID)) {
            existingDecision = decisionTableEntityManager.findLatestDecisionByKeyAndTenantId(key, tenantId);
        } else {
            existingDecision = decisionTableEntityManager.findLatestDecisionByKey(key);
        }

        return existingDecision;
    }

    /**
     * Gets the persisted version of the already-deployed decision. Note that this is different from {@link #getMostRecentVersionOfDecision} as it looks specifically for a decision
     * that is already persisted and attached to a particular deployment, rather than the latest version across all deployments.
     */
    public DecisionEntity getPersistedInstanceOfDecision(DecisionEntity decision) {
        String deploymentId = decision.getDeploymentId();
        if (StringUtils.isEmpty(decision.getDeploymentId())) {
            throw new FlowableIllegalArgumentException("Provided decision must have a deployment id.");
        }

        DecisionEntityManager decisionEntityManager = CommandContextUtil.getDmnEngineConfiguration().getDecisionEntityManager();
        DecisionEntity persistedDecision;
        if (decision.getTenantId() == null || DmnEngineConfiguration.NO_TENANT_ID.equals(decision.getTenantId())) {
            persistedDecision = decisionEntityManager.findDecisionByDeploymentAndKey(deploymentId, decision.getKey());
        } else {
            persistedDecision = decisionEntityManager.findDecisionByDeploymentAndKeyAndTenantId(deploymentId, decision.getKey(), decision.getTenantId());
        }

        return persistedDecision;
    }
}
