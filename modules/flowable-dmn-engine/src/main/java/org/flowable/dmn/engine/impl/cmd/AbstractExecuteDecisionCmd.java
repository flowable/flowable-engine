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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.ExecuteDecisionContext;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.DmnDeploymentQueryImpl;
import org.flowable.dmn.engine.impl.ExecuteDecisionBuilderImpl;
import org.flowable.dmn.engine.impl.persistence.deploy.DecisionCacheEntry;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityManager;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DmnDefinition;

/**
 * @author Yvo Swillens
 */
public abstract class AbstractExecuteDecisionCmd implements Serializable {

    private static final long serialVersionUID = 1L;

    protected ExecuteDecisionContext executeDecisionContext;

    public AbstractExecuteDecisionCmd(ExecuteDecisionContext executeDecisionContext) {
        this.executeDecisionContext = executeDecisionContext;
    }

    public AbstractExecuteDecisionCmd(ExecuteDecisionBuilderImpl definitionBuilder) {
        executeDecisionContext = new ExecuteDecisionContext();
        executeDecisionContext.setDecisionKey(definitionBuilder.getDecisionKey());
        executeDecisionContext.setParentDeploymentId(definitionBuilder.getParentDeploymentId());
        executeDecisionContext.setInstanceId(definitionBuilder.getInstanceId());
        executeDecisionContext.setExecutionId(definitionBuilder.getExecutionId());
        executeDecisionContext.setActivityId(definitionBuilder.getActivityId());
        executeDecisionContext.setScopeType(definitionBuilder.getScopeType());
        executeDecisionContext.setVariables(definitionBuilder.getVariables());
        executeDecisionContext.setTenantId(definitionBuilder.getTenantId());
        executeDecisionContext.setFallbackToDefaultTenant(definitionBuilder.isFallbackToDefaultTenant());
    }

    public AbstractExecuteDecisionCmd(String decisionKey, Map<String, Object> variables) {
        executeDecisionContext = new ExecuteDecisionContext();
        executeDecisionContext.setDecisionKey(decisionKey);
        executeDecisionContext.setVariables(variables);
    }

    protected DmnDefinition resolveDefinition() {
        DmnDecision decision = null;
        DmnEngineConfiguration dmnEngineConfiguration = CommandContextUtil.getDmnEngineConfiguration();
        DecisionEntityManager decisionEntityManager = dmnEngineConfiguration.getDecisionEntityManager();

        String decisionKey = executeDecisionContext.getDecisionKey();
        String parentDeploymentId = executeDecisionContext.getParentDeploymentId();
        String tenantId = executeDecisionContext.getTenantId();

        if (StringUtils.isNotEmpty(decisionKey) && StringUtils.isNotEmpty(parentDeploymentId) &&
                        !dmnEngineConfiguration.isAlwaysLookupLatestDefinitionVersion() && StringUtils.isNotEmpty(tenantId)) {
            
            List<DmnDeployment> dmnDeployments = CommandContextUtil.getDeploymentEntityManager().findDeploymentsByQueryCriteria(
                new DmnDeploymentQueryImpl().parentDeploymentId(parentDeploymentId));

            if (dmnDeployments != null && dmnDeployments.size() != 0) {
                decision = decisionEntityManager.findDecisionByDeploymentAndKeyAndTenantId(
                    dmnDeployments.get(0).getId(), decisionKey, tenantId);
            }

            if (decision == null) {
                // If there is no decision table found linked to the deployment id, try to find one without a specific deployment id.
                decision = decisionEntityManager.findLatestDecisionByKeyAndTenantId(decisionKey, tenantId);

                if (decision == null) {
                    // if fallback to default tenant is enabled do a final lookup query
                    if (executeDecisionContext.isFallbackToDefaultTenant() || dmnEngineConfiguration.isFallbackToDefaultTenant()) {
                        String defaultTenant = dmnEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(tenantId, ScopeTypes.DMN, decisionKey);
                        if (StringUtils.isNotEmpty(defaultTenant)) {
                            decision = decisionEntityManager.findLatestDecisionByKeyAndTenantId(decisionKey, defaultTenant);
                            if (decision == null) {
                                throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey +
                                    ". There was also no fall back decision found for default tenant " + defaultTenant);
                            }
                            
                        } else {
                            decision = decisionEntityManager.findLatestDecisionByKey(decisionKey);
                            if (decision == null) {
                                throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey +
                                    ". There was also no fall back decision table found without tenant.");
                            }
                        }
                        
                    } else {
                        throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey +
                            ", parent deployment id " + parentDeploymentId + " and tenant id: " + tenantId +
                            ". There was also no fall back decision found without parent deployment id.");
                    }
                }
            }
            
        } else if (StringUtils.isNotEmpty(decisionKey) && StringUtils.isNotEmpty(parentDeploymentId) &&
                        !dmnEngineConfiguration.isAlwaysLookupLatestDefinitionVersion()) {
            
            List<DmnDeployment> dmnDeployments = CommandContextUtil.getDeploymentEntityManager().findDeploymentsByQueryCriteria(
                new DmnDeploymentQueryImpl().parentDeploymentId(parentDeploymentId));

            if (dmnDeployments != null && dmnDeployments.size() != 0) {
                decision = decisionEntityManager.findDecisionByDeploymentAndKey(dmnDeployments.get(0).getId(), decisionKey);
            }

            if (decision == null) {
                // If there is no decision table found linked to the deployment id, try to find one without a specific deployment id.
                decision = decisionEntityManager.findLatestDecisionByKey(decisionKey);

                if (decision == null) {
                    throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey +
                        " and parent deployment id " + parentDeploymentId +
                        ". There was also no fall back decision found without parent deployment id.");
                }
            }
            
        } else if (StringUtils.isNotEmpty(decisionKey) && StringUtils.isNotEmpty(tenantId)) {
            decision = decisionEntityManager.findLatestDecisionByKeyAndTenantId(decisionKey, tenantId);
            if (decision == null) {
                if (executeDecisionContext.isFallbackToDefaultTenant() || dmnEngineConfiguration.isFallbackToDefaultTenant()) {
                    String defaultTenant = dmnEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(tenantId, ScopeTypes.DMN, decisionKey);
                    if (StringUtils.isNotEmpty(defaultTenant)) {
                        decision = decisionEntityManager.findLatestDecisionByKeyAndTenantId(decisionKey, defaultTenant);
                        if (decision == null) {
                            throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey +
                                ". There was also no fall back decision found for default tenant " +
                                defaultTenant + ".");
                        }

                    } else {
                        decision = decisionEntityManager.findLatestDecisionByKey(decisionKey);
                        if (decision == null) {
                            throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey +
                                ". There was also no fall back decision found without tenant.");
                        }
                    }

                } else {
                    throw new FlowableObjectNotFoundException(
                        "No decision found for key: " + decisionKey + " and tenantId: " + tenantId + ".");
                }
            }

        } else if (StringUtils.isNotEmpty(decisionKey)) {
            decision = decisionEntityManager.findLatestDecisionByKey(decisionKey);
            if (decision == null) {
                throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey + ".");
            }
            
        } else {
            throw new FlowableIllegalArgumentException("decisionKey is null");
        }

        executeDecisionContext.setDecisionId(decision.getId());
        executeDecisionContext.setDecisionVersion(decision.getVersion());
        executeDecisionContext.setDeploymentId(decision.getDeploymentId());

        DecisionCacheEntry decisionTableCacheEntry = CommandContextUtil.getDmnEngineConfiguration().getDeploymentManager().resolveDecision(decision);
        DmnDefinition dmnDefinition = decisionTableCacheEntry.getDmnDefinition();

        return dmnDefinition;
    }

    protected void execute(CommandContext commandContext, DmnDefinition definition) {
        Decision decision = definition.getDecisionById(executeDecisionContext.getDecisionKey());

        if (decision == null) {
            throw new FlowableIllegalArgumentException("no decision with id: '" + executeDecisionContext.getDecisionKey() + "' found in definition");
        }

        executeDecisionContext.setDmnElement(decision);
        CommandContextUtil.getAgenda(commandContext).planExecuteDecisionOperation(executeDecisionContext, decision);
    }
}
