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
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.engine.impl.DmnDeploymentQueryImpl;
import org.flowable.dmn.engine.impl.ExecuteDecisionBuilderImpl;
import org.flowable.dmn.engine.impl.ExecuteDecisionInfo;
import org.flowable.dmn.engine.impl.persistence.deploy.DecisionTableCacheEntry;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionTableEntityManager;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.Decision;

/**
 * @author Yvo Swillens
 */
public abstract class AbstractExecuteDecisionCmd implements Serializable {

    private static final long serialVersionUID = 1L;

    protected ExecuteDecisionInfo executeDecisionInfo = new ExecuteDecisionInfo();

    public AbstractExecuteDecisionCmd(ExecuteDecisionBuilderImpl decisionBuilder) {
        executeDecisionInfo.setDecisionKey(decisionBuilder.getDecisionKey());
        executeDecisionInfo.setParentDeploymentId(decisionBuilder.getParentDeploymentId());
        executeDecisionInfo.setInstanceId(decisionBuilder.getInstanceId());
        executeDecisionInfo.setExecutionId(decisionBuilder.getExecutionId());
        executeDecisionInfo.setActivityId(decisionBuilder.getActivityId());
        executeDecisionInfo.setScopeType(decisionBuilder.getScopeType());
        executeDecisionInfo.setVariables(decisionBuilder.getVariables());
        executeDecisionInfo.setTenantId(decisionBuilder.getTenantId());
        executeDecisionInfo.setFallbackToDefaultTenant(decisionBuilder.isFallbackToDefaultTenant());
    }

    public AbstractExecuteDecisionCmd(String decisionKey, Map<String, Object> variables) {
        executeDecisionInfo.setDecisionKey(decisionKey);
        executeDecisionInfo.setVariables(variables);
    }


    protected DmnDecisionTable resolveDecisionTable() {
        DmnDecisionTable decisionTable = null;
        DecisionTableEntityManager decisionTableManager = CommandContextUtil.getDmnEngineConfiguration().getDecisionTableEntityManager();

        String decisionKey = executeDecisionInfo.getDecisionKey();
        String parentDeploymentId = executeDecisionInfo.getParentDeploymentId();
        String tenantId = executeDecisionInfo.getTenantId();
        
        if (StringUtils.isNotEmpty(decisionKey) && StringUtils.isNotEmpty(parentDeploymentId) && StringUtils.isNotEmpty(tenantId)) {
            List<DmnDeployment> dmnDeployments = CommandContextUtil.getDeploymentEntityManager().findDeploymentsByQueryCriteria(
                new DmnDeploymentQueryImpl().parentDeploymentId(parentDeploymentId));

            if (dmnDeployments != null && dmnDeployments.size() != 0) {
                decisionTable = decisionTableManager.findDecisionTableByDeploymentAndKeyAndTenantId(
                    dmnDeployments.get(0).getId(), decisionKey, tenantId);
            }

            if (decisionTable == null) {
                // If there is no decision table found linked to the deployment id, try to find one without a specific deployment id.
                decisionTable = decisionTableManager.findLatestDecisionTableByKeyAndTenantId(decisionKey, tenantId);

                if (decisionTable == null) {
                    // if fallback to default tenant is enabled do a final lookup query
                    if (executeDecisionInfo.isFallbackToDefaultTenant()) {
                        decisionTable = decisionTableManager.findLatestDecisionTableByKey(decisionKey);
                        if (decisionTable == null) {
                            throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey +
                                ". There was also no fall back decision table found without tenant.");
                        }
                        
                    } else {
                        throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey +
                            ", parent deployment id " + parentDeploymentId + " and tenant id: " + tenantId +
                            ". There was also no fall back decision table found without parent deployment id.");
                    }
                }
            }
            
        } else if (StringUtils.isNotEmpty(decisionKey) && StringUtils.isNotEmpty(parentDeploymentId)) {
            List<DmnDeployment> dmnDeployments = CommandContextUtil.getDeploymentEntityManager().findDeploymentsByQueryCriteria(
                new DmnDeploymentQueryImpl().parentDeploymentId(parentDeploymentId));

            if (dmnDeployments != null && dmnDeployments.size() != 0) {
                decisionTable = decisionTableManager.findDecisionTableByDeploymentAndKey(dmnDeployments.get(0).getId(), decisionKey);
            }

            if (decisionTable == null) {
                // If there is no decision table found linked to the deployment id, try to find one without a specific deployment id.
                decisionTable = decisionTableManager.findLatestDecisionTableByKey(decisionKey);

                if (decisionTable == null) {
                    throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey +
                        " and parent deployment id " + parentDeploymentId +
                        ". There was also no fall back decision table found without parent deployment id.");
                }
            }
        } else if (StringUtils.isNotEmpty(decisionKey) && StringUtils.isNotEmpty(tenantId)) {
            decisionTable = decisionTableManager.findLatestDecisionTableByKeyAndTenantId(decisionKey, tenantId);
            if (decisionTable == null) {
                if (executeDecisionInfo.isFallbackToDefaultTenant()) {
                    decisionTable = decisionTableManager.findLatestDecisionTableByKey(decisionKey);
                    if (decisionTable == null) {
                        throw new FlowableObjectNotFoundException("No decision found for key: " + decisionKey +
                            ". There was also no fall back decision table found without tenant.");                    }
                } else {
                    throw new FlowableObjectNotFoundException(
                        "Decision table for key [" + decisionKey + "] and tenantId [" + tenantId + "] was not found");
                }
            }

        } else if (StringUtils.isNotEmpty(decisionKey)) {
            decisionTable = decisionTableManager.findLatestDecisionTableByKey(decisionKey);
            if (decisionTable == null) {
                throw new FlowableObjectNotFoundException("Decision table for key [" + decisionKey + "] was not found");
            }
            
        } else {
            throw new FlowableIllegalArgumentException("decisionKey is null");
        }

        executeDecisionInfo.setDecisionDefinitionId(decisionTable.getId());
        executeDecisionInfo.setDeploymentId(decisionTable.getDeploymentId());

        return decisionTable;
    }

    protected Decision resolveDecision(DmnDecisionTable decisionTable) {
        if (decisionTable == null) {
            throw new FlowableIllegalArgumentException("decisionTable is null");
        }

        DecisionTableCacheEntry decisionTableCacheEntry = CommandContextUtil.getDmnEngineConfiguration().getDeploymentManager().resolveDecisionTable(decisionTable);
        Decision decision = decisionTableCacheEntry.getDecision();

        return decision;
    }
}
