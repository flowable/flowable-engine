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
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.DmnDeploymentQueryImpl;
import org.flowable.dmn.engine.impl.ExecuteDecisionBuilderImpl;
import org.flowable.dmn.api.ExecuteDecisionContext;
import org.flowable.dmn.engine.impl.persistence.deploy.DecisionCacheEntry;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityManager;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
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
        DmnDecision definition = null;
        DmnEngineConfiguration dmnEngineConfiguration = CommandContextUtil.getDmnEngineConfiguration();
        DecisionEntityManager definitionManager = dmnEngineConfiguration.getDecisionEntityManager();

        String definitionKey = executeDecisionContext.getDecisionKey();
        String parentDeploymentId = executeDecisionContext.getParentDeploymentId();
        String tenantId = executeDecisionContext.getTenantId();

        if (StringUtils.isNotEmpty(definitionKey) && StringUtils.isNotEmpty(parentDeploymentId) &&
                        !dmnEngineConfiguration.isAlwaysLookupLatestDefinitionVersion() && StringUtils.isNotEmpty(tenantId)) {
            
            List<DmnDeployment> dmnDeployments = CommandContextUtil.getDeploymentEntityManager().findDeploymentsByQueryCriteria(
                new DmnDeploymentQueryImpl().parentDeploymentId(parentDeploymentId));

            if (dmnDeployments != null && dmnDeployments.size() != 0) {
                definition = definitionManager.findDecisionByDeploymentAndKeyAndTenantId(
                    dmnDeployments.get(0).getId(), definitionKey, tenantId);
            }

            if (definition == null) {
                // If there is no decision table found linked to the deployment id, try to find one without a specific deployment id.
                definition = definitionManager.findLatestDecisionByKeyAndTenantId(definitionKey, tenantId);

                if (definition == null) {
                    // if fallback to default tenant is enabled do a final lookup query
                    if (executeDecisionContext.isFallbackToDefaultTenant() || dmnEngineConfiguration.isFallbackToDefaultTenant()) {
                        String defaultTenant = dmnEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(tenantId, ScopeTypes.DMN, definitionKey);
                        if (StringUtils.isNotEmpty(defaultTenant)) {
                            definition = definitionManager.findLatestDecisionByKeyAndTenantId(definitionKey, defaultTenant);
                            if (definition == null) {
                                throw new FlowableObjectNotFoundException("No decision found for key: " + definitionKey +
                                    ". There was also no fall back decision found for default tenant " + defaultTenant);
                            }
                            
                        } else {
                            definition = definitionManager.findLatestDecisionByKey(definitionKey);
                            if (definition == null) {
                                throw new FlowableObjectNotFoundException("No decision found for key: " + definitionKey +
                                    ". There was also no fall back decision table found without tenant.");
                            }
                        }
                        
                    } else {
                        throw new FlowableObjectNotFoundException("No decision found for key: " + definitionKey +
                            ", parent deployment id " + parentDeploymentId + " and tenant id: " + tenantId +
                            ". There was also no fall back decision found without parent deployment id.");
                    }
                }
            }
            
        } else if (StringUtils.isNotEmpty(definitionKey) && StringUtils.isNotEmpty(parentDeploymentId) &&
                        !dmnEngineConfiguration.isAlwaysLookupLatestDefinitionVersion()) {
            
            List<DmnDeployment> dmnDeployments = CommandContextUtil.getDeploymentEntityManager().findDeploymentsByQueryCriteria(
                new DmnDeploymentQueryImpl().parentDeploymentId(parentDeploymentId));

            if (dmnDeployments != null && dmnDeployments.size() != 0) {
                definition = definitionManager.findDecisionByDeploymentAndKey(dmnDeployments.get(0).getId(), definitionKey);
            }

            if (definition == null) {
                // If there is no decision table found linked to the deployment id, try to find one without a specific deployment id.
                definition = definitionManager.findLatestDecisionByKey(definitionKey);

                if (definition == null) {
                    throw new FlowableObjectNotFoundException("No decision found for key: " + definitionKey +
                        " and parent deployment id " + parentDeploymentId +
                        ". There was also no fall back decision found without parent deployment id.");
                }
            }
            
        } else if (StringUtils.isNotEmpty(definitionKey) && StringUtils.isNotEmpty(tenantId)) {
            definition = definitionManager.findLatestDecisionByKeyAndTenantId(definitionKey, tenantId);
            if (definition == null) {
                if (executeDecisionContext.isFallbackToDefaultTenant() || dmnEngineConfiguration.isFallbackToDefaultTenant()) {
                    String defaultTenant = dmnEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(tenantId, ScopeTypes.DMN, definitionKey);
                    if (StringUtils.isNotEmpty(defaultTenant)) {
                        definition = definitionManager.findLatestDecisionByKeyAndTenantId(definitionKey, defaultTenant);
                        if (definition == null) {
                            throw new FlowableObjectNotFoundException("No decision found for key: " + definitionKey +
                                ". There was also no fall back decision found for default tenant " +
                                    defaultTenant + ".");
                        }
                        
                    } else {
                        definition = definitionManager.findLatestDecisionByKey(definitionKey);
                        if (definition == null) {
                            throw new FlowableObjectNotFoundException("No decision found for key: " + definitionKey +
                                ". There was also no fall back decision found without tenant.");
                        }
                    }
                    
                } else {
                    throw new FlowableObjectNotFoundException(
                        "Decision for key [" + definitionKey + "] and tenantId [" + tenantId + "] was not found");
                }
            }

        } else if (StringUtils.isNotEmpty(definitionKey)) {
            definition = definitionManager.findLatestDecisionByKey(definitionKey);
            if (definition == null) {
                throw new FlowableObjectNotFoundException("Decision for key [" + definitionKey + "] was not found");
            }
            
        } else {
            throw new FlowableIllegalArgumentException("decisionKey is null");
        }

        executeDecisionContext.setDecisionId(definition.getId());
        executeDecisionContext.setDecisionVersion(definition.getVersion());
        executeDecisionContext.setDeploymentId(definition.getDeploymentId());

        DecisionCacheEntry decisionTableCacheEntry = CommandContextUtil.getDmnEngineConfiguration().getDeploymentManager().resolveDecision(definition);
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
