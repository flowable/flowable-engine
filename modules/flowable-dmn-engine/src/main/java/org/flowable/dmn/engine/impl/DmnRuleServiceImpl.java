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

import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.api.RuleEngineExecutionResult;
import org.flowable.dmn.api.RuleEngineExecutionSingleResult;
import org.flowable.dmn.engine.impl.cmd.ExecuteDecisionCmd;
import org.flowable.dmn.engine.impl.cmd.ExecuteDecisionSingleResultCmd;
import org.flowable.dmn.engine.impl.cmd.ExecuteDecisionSingleResultWithAuditTrailCmd;
import org.flowable.dmn.engine.impl.cmd.ExecuteDecisionWithAuditTrailCmd;

/**
 * @author Yvo Swillens
 */
public class DmnRuleServiceImpl extends ServiceImpl implements DmnRuleService {

    @Override
    public List<Map<String, Object>> executeDecisionByKey(String decisionKey, Map<String, Object> inputVariables) {
        return commandExecutor.execute(new ExecuteDecisionCmd(decisionKey, inputVariables));
    }

    @Override
    public Map<String, Object> executeDecisionByKeySingleResult(String decisionKey, Map<String, Object> inputVariables) {
        return commandExecutor.execute(new ExecuteDecisionSingleResultCmd(decisionKey, inputVariables));
    }

    @Override
    public RuleEngineExecutionResult executeDecisionByKeyWithAuditTrail(String decisionKey, Map<String, Object> inputVariables) {
        return commandExecutor.execute(new ExecuteDecisionWithAuditTrailCmd(decisionKey, inputVariables));
    }

    @Override
    public RuleEngineExecutionSingleResult executeDecisionByKeySingleResultWithAuditTrail(String decisionKey, Map<String, Object> inputVariables) {
        return commandExecutor.execute(new ExecuteDecisionSingleResultWithAuditTrailCmd(decisionKey, inputVariables));

    }

    @Override
    public List<Map<String, Object>> executeDecisionByKeyAndTenantId(String decisionKey, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionCmd(decisionKey, null, inputVariables, tenantId));
    }

    @Override
    public Map<String, Object> executeDecisionByKeyAndTenantIdSingleResult(String decisionKey, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionSingleResultCmd(decisionKey, null, inputVariables, tenantId));
    }

    @Override
    public RuleEngineExecutionResult executeDecisionByKeyAndTenantIdWithAuditTrail(String decisionKey, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionWithAuditTrailCmd(decisionKey, null, inputVariables, tenantId));
    }

    @Override
    public RuleEngineExecutionSingleResult executeDecisionByKeyAndTenantIdWithAuditTrailSingleResult(String decisionKey, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionSingleResultWithAuditTrailCmd(decisionKey, null, inputVariables, tenantId));
    }

    @Override
    public List<Map<String, Object>> executeDecisionByKeyAndParentDeploymentId(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables) {
        return commandExecutor.execute(new ExecuteDecisionCmd(decisionKey, parentDeploymentId, inputVariables));
    }

    @Override
    public Map<String, Object> executeDecisionByKeyAndParentDeploymentIdSingleResult(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables) {
        return commandExecutor.execute(new ExecuteDecisionSingleResultCmd(decisionKey, parentDeploymentId, inputVariables));
    }

    @Override
    public RuleEngineExecutionResult executeDecisionByKeyAndParentDeploymentIdWithAuditTrail(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionWithAuditTrailCmd(decisionKey, parentDeploymentId, inputVariables, tenantId));
    }

    @Override
    public RuleEngineExecutionSingleResult executeDecisionByKeyAndParentDeploymentIdWithAuditTrailSingleResult(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionSingleResultWithAuditTrailCmd(decisionKey, parentDeploymentId, inputVariables, tenantId));
    }

    @Override
    public List<Map<String, Object>> executeDecisionByKeyParentDeploymentIdAndTenantId(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionCmd(decisionKey, parentDeploymentId, inputVariables, tenantId));
    }

    @Override
    public Map<String, Object> executeDecisionByKeyParentDeploymentIdAndTenantIdSingleResult(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionSingleResultCmd(decisionKey, parentDeploymentId, inputVariables, tenantId));
    }

    @Override
    public RuleEngineExecutionResult executeDecisionByKeyParentDeploymentIdAndTenantIdWithAuditTrail(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionWithAuditTrailCmd(decisionKey, parentDeploymentId, inputVariables, tenantId));
    }

    @Override
    public RuleEngineExecutionSingleResult executeDecisionByKeyParentDeploymentIdAndTenantIdWithAuditTrailSingleResult(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId) {
        return commandExecutor.execute(new ExecuteDecisionSingleResultWithAuditTrailCmd(decisionKey, parentDeploymentId, inputVariables, tenantId));
    }
}