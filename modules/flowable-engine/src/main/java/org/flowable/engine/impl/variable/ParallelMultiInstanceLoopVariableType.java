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
package org.flowable.engine.impl.variable;

import java.util.List;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;

/**
 * @author Filip Hrisafov
 */
public class ParallelMultiInstanceLoopVariableType implements VariableType {

    public static final String TYPE_NAME = "bpmnParallelMultiInstanceCompleted";
    protected static final String NUMBER_OF_INSTANCES = "nrOfInstances";

    protected final ProcessEngineConfigurationImpl processEngineConfiguration;

    public ParallelMultiInstanceLoopVariableType(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public boolean isCachable() {
        return false;
    }

    @Override
    public boolean isAbleToStore(Object value) {
        return value instanceof ParallelMultiInstanceLoopVariable;
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        if (value instanceof ParallelMultiInstanceLoopVariable) {
            valueFields.setTextValue(((ParallelMultiInstanceLoopVariable) value).getExecutionId());
            valueFields.setTextValue2(((ParallelMultiInstanceLoopVariable) value).getType());
        } else {
            valueFields.setTextValue(null);
            valueFields.setTextValue2(null);
        }
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        if (commandContext != null) {
            return getValue(valueFields, commandContext);
        } else {
            return processEngineConfiguration.getCommandExecutor()
                    .execute(context -> getValue(valueFields, context));
        }
    }

    protected Object getValue(ValueFields valueFields, CommandContext commandContext) {
        String multiInstanceRootId = valueFields.getTextValue();
        String type = valueFields.getTextValue2();

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExecutionEntityManager executionEntityManager = processEngineConfiguration.getExecutionEntityManager();
        ExecutionEntity multiInstanceRootExecution = executionEntityManager.findById(multiInstanceRootId);
        List<? extends ExecutionEntity> childExecutions = multiInstanceRootExecution.getExecutions();
        int nrOfActiveInstances = (int) childExecutions.stream().filter(execution -> execution.isActive()
            && !(execution.getCurrentFlowElement() instanceof BoundaryEvent)).count();
        if (ParallelMultiInstanceLoopVariable.COMPLETED_INSTANCES.equals(type)) {
            Object nrOfInstancesValue = multiInstanceRootExecution.getVariable(NUMBER_OF_INSTANCES);
            int nrOfInstances = (Integer) (nrOfInstancesValue != null ? nrOfInstancesValue : 0);
            return nrOfInstances - nrOfActiveInstances;
        } else if (ParallelMultiInstanceLoopVariable.ACTIVE_INSTANCES.equals(type)) {
            return nrOfActiveInstances;
        } else {
            //TODO maybe throw exception
            return 0;
        }
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
