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

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.api.ExecuteDecisionBuilder;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.cmd.ExecuteDecisionCmd;
import org.flowable.dmn.engine.impl.cmd.ExecuteDecisionSingleResultCmd;
import org.flowable.dmn.engine.impl.cmd.ExecuteDecisionWithAuditTrailCmd;

/**
 * @author Yvo Swillens
 */
public class DmnRuleServiceImpl extends CommonEngineServiceImpl<DmnEngineConfiguration> implements DmnRuleService {
    
    @Override
    public ExecuteDecisionBuilder createExecuteDecisionBuilder() {
        return new ExecuteDecisionBuilderImpl(this);
    }
    
    @Override
    @Deprecated
    public List<Map<String, Object>> executeDecisionByKey(String decisionKey, Map<String, Object> inputVariables) {
        return commandExecutor.execute(new ExecuteDecisionCmd(decisionKey, inputVariables));
    }

    @Override
    @Deprecated
    public Map<String, Object> executeDecisionByKeySingleResult(String decisionKey, Map<String, Object> inputVariables) {
        return commandExecutor.execute(new ExecuteDecisionSingleResultCmd(decisionKey, inputVariables));
    }

    @Override
    @Deprecated
    public DecisionExecutionAuditContainer executeDecisionByKeyWithAuditTrail(String decisionKey, Map<String, Object> inputVariables) {
        return commandExecutor.execute(new ExecuteDecisionWithAuditTrailCmd(decisionKey, inputVariables));
    }

    @Override
    @Deprecated
    public List<Map<String, Object>> executeDecisionByKeyAndTenantId(String decisionKey, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionCmd(decisionKey, null, inputVariables, tenantId));
    }

    @Override
    @Deprecated
    public Map<String, Object> executeDecisionByKeyAndTenantIdSingleResult(String decisionKey, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionSingleResultCmd(decisionKey, null, inputVariables, tenantId));
    }

    @Override
    @Deprecated
    public DecisionExecutionAuditContainer executeDecisionByKeyAndTenantIdWithAuditTrail(String decisionKey, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionWithAuditTrailCmd(decisionKey, null, inputVariables, tenantId));
    }

    @Override
    @Deprecated
    public List<Map<String, Object>> executeDecisionByKeyAndParentDeploymentId(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables) {
        return commandExecutor.execute(new ExecuteDecisionCmd(decisionKey, parentDeploymentId, inputVariables));
    }

    @Override
    @Deprecated
    public Map<String, Object> executeDecisionByKeyAndParentDeploymentIdSingleResult(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables) {
        return commandExecutor.execute(new ExecuteDecisionSingleResultCmd(decisionKey, parentDeploymentId, inputVariables));
    }

    @Override
    @Deprecated
    public DecisionExecutionAuditContainer executeDecisionByKeyAndParentDeploymentIdWithAuditTrail(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionWithAuditTrailCmd(decisionKey, parentDeploymentId, inputVariables, tenantId));
    }
    
    @Override
    @Deprecated
    public List<Map<String, Object>> executeDecisionByKeyParentDeploymentIdAndTenantId(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionCmd(decisionKey, parentDeploymentId, inputVariables, tenantId));
    }
    
    @Override
    @Deprecated
    public Map<String, Object> executeDecisionByKeyParentDeploymentIdAndTenantIdSingleResult(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionSingleResultCmd(decisionKey, parentDeploymentId, inputVariables, tenantId));
    }
    
    @Override
    @Deprecated
    public DecisionExecutionAuditContainer executeDecisionByKeyParentDeploymentIdAndTenantIdWithAuditTrail(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionWithAuditTrailCmd(decisionKey, parentDeploymentId, inputVariables, tenantId));
    }

    public List<Map<String, Object>> executeDecision(ExecuteDecisionBuilderImpl executeDecisionBuilder) {
        return commandExecutor.execute(new ExecuteDecisionCmd(executeDecisionBuilder));
    }
    
    public Map<String, Object> executeDecisionWithSingleResult(ExecuteDecisionBuilderImpl executeDecisionBuilder) {
        return commandExecutor.execute(new ExecuteDecisionSingleResultCmd(executeDecisionBuilder));
    }
    
    public DecisionExecutionAuditContainer executeDecisionWithAuditTrail(ExecuteDecisionBuilderImpl executeDecisionBuilder) {
        return commandExecutor.execute(new ExecuteDecisionWithAuditTrailCmd(executeDecisionBuilder));
    }
}