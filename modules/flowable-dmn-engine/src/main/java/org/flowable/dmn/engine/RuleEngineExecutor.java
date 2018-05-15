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
package org.flowable.dmn.engine;

import java.util.Map;

import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.engine.impl.ExecuteDecisionInfo;
import org.flowable.dmn.engine.impl.hitpolicy.AbstractHitPolicy;
import org.flowable.dmn.model.Decision;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Yvo Swillens
 */
public interface RuleEngineExecutor {

    DecisionExecutionAuditContainer execute(Decision decision, ExecuteDecisionInfo executeDecisionInfo);
    
    Map<String, AbstractHitPolicy> getHitPolicyBehaviors();

    void setHitPolicyBehaviors(Map<String, AbstractHitPolicy> hitPolicyBehaviors);
    
    ExpressionManager getExpressionManager();

    void setExpressionManager(ExpressionManager expressionManager);
    
    ObjectMapper getObjectMapper();

    void setObjectMapper(ObjectMapper objectMapper);
}
