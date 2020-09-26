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
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

public class IntermediateCatchConditionalEventActivityBehavior extends IntermediateCatchEventActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected ConditionalEventDefinition conditionalEventDefinition;
    protected String conditionExpression;

    public IntermediateCatchConditionalEventActivityBehavior(ConditionalEventDefinition conditionalEventDefinition, String conditionExpression) {
        this.conditionalEventDefinition = conditionalEventDefinition;
        this.conditionExpression = conditionExpression;
    }

    @Override
    public void execute(DelegateExecution execution) {
        ExecutionEntity executionEntity = (ExecutionEntity) execution;

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createConditionalEvent(FlowableEngineEventType.ACTIVITY_CONDITIONAL_WAITING, 
                    executionEntity.getActivityId(), conditionExpression, executionEntity.getId(), 
                    executionEntity.getProcessInstanceId(), executionEntity.getProcessDefinitionId()),
                    processEngineConfiguration.getEngineCfgKey());
        }
    }

    @Override
    public void trigger(DelegateExecution execution, String triggerName, Object triggerData) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        Expression expression = processEngineConfiguration.getExpressionManager().createExpression(conditionExpression);
        Object result = expression.getValue(execution);
        
        if (result instanceof Boolean && (Boolean) result) {
            ExecutionEntity executionEntity = (ExecutionEntity) execution;
            FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
            if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createConditionalEvent(FlowableEngineEventType.ACTIVITY_CONDITIONAL_RECEIVED, executionEntity.getActivityId(), 
                        conditionExpression, executionEntity.getId(), executionEntity.getProcessInstanceId(), executionEntity.getProcessDefinitionId()),
                        processEngineConfiguration.getEngineCfgKey());
            }
            
            leaveIntermediateCatchEvent(execution);
        }
    }
}
