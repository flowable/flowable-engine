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

import java.util.Map;

import org.flowable.dmn.api.DmnRuleService;
import org.flowable.dmn.api.RuleEngineExecutionResult;
import org.flowable.dmn.engine.RuleEngineExecutor;
import org.flowable.dmn.engine.impl.cmd.ExecuteDecisionCmd;

/**
 * @author Yvo Swillens
 */
public class DmnRuleServiceImpl extends ServiceImpl implements DmnRuleService {

  protected RuleEngineExecutor ruleEngineExecutor;

  @Override
  public RuleEngineExecutionResult executeDecisionByKey(String decisionKey, Map<String, Object> variables) {
    return commandExecutor.execute(new ExecuteDecisionCmd(decisionKey, variables));
  }

  @Override
  public RuleEngineExecutionResult executeDecisionByKeyAndTenantId(String decisionKey, Map<String, Object> variables, String tenantId) {
    return commandExecutor.execute(new ExecuteDecisionCmd(decisionKey, null, variables, tenantId));
  }
  
  @Override
  public RuleEngineExecutionResult executeDecisionByKeyAndParentDeploymentId(String decisionKey, String parentDeploymentId, Map<String, Object> variables) {
    return commandExecutor.execute(new ExecuteDecisionCmd(decisionKey, parentDeploymentId, variables));
  }
  
  @Override
  public RuleEngineExecutionResult executeDecisionByKeyParentDeploymentIdAndTenantId(String decisionKey, String parentDeploymentId, 
      Map<String, Object> variables, String tenantId) {
    
    return commandExecutor.execute(new ExecuteDecisionCmd(decisionKey, parentDeploymentId, variables, tenantId));
  }
}