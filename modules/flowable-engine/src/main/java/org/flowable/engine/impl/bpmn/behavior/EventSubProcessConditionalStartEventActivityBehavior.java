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

package org.flowable.engine.impl.bpmn.behavior;

import org.flowable.bpmn.model.ConditionalEventDefinition;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * Implementation of the BPMN 2.0 event subprocess start event.
 * 
 * @author Tijs Rademakers
 */
public class EventSubProcessConditionalStartEventActivityBehavior extends FlowNodeActivityBehavior {

    private static final long serialVersionUID = 1L;
    
    protected ConditionalEventDefinition conditionalEventDefinition;
    protected String conditionExpression;
    
    public EventSubProcessConditionalStartEventActivityBehavior(ConditionalEventDefinition conditionalEventDefinition, String conditionExpression) {
        this.conditionalEventDefinition = conditionalEventDefinition;
        this.conditionExpression = conditionExpression;
    }
    
    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        CommandContext commandContext = Context.getCommandContext();
        
        Expression expression = CommandContextUtil.getProcessEngineConfiguration(commandContext).getExpressionManager().createExpression(conditionExpression);
        Object result = expression.getValue(execution);
        if (result instanceof Boolean && (Boolean) result) {
            CommandContextUtil.getActivityInstanceEntityManager(commandContext).recordActivityStart((ExecutionEntity) execution);
            super.trigger(execution, signalName, signalData);
        }
    }

}
