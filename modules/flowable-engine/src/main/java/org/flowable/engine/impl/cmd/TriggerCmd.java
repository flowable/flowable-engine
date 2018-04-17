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

package org.flowable.engine.impl.cmd;

import java.util.Map;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class TriggerCmd extends NeedsActiveExecutionCmd<Object> {

    private static final long serialVersionUID = 1L;

    protected Map<String, Object> processVariables;
    protected Map<String, Object> transientVariables;
    protected boolean async;

    public TriggerCmd(String executionId, Map<String, Object> processVariables) {
        super(executionId);
        this.processVariables = processVariables;
    }

    public TriggerCmd(String executionId, Map<String, Object> processVariables, boolean async) {
        super(executionId);
        this.processVariables = processVariables;
        this.async = async;
    }

    public TriggerCmd(String executionId, Map<String, Object> processVariables, Map<String, Object> transientVariables) {
        this(executionId, processVariables);
        this.transientVariables = transientVariables;
    }

    @Override
    protected Object execute(CommandContext commandContext, ExecutionEntity execution) {
        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, execution.getProcessDefinitionId())) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            compatibilityHandler.trigger(executionId, processVariables, transientVariables);
            return null;
        }
        
        if (processVariables != null) {
            execution.setVariables(processVariables);
        }

        if (!async) {
            if (transientVariables != null) {
                execution.setTransientVariables(transientVariables);
            }

            CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
                    FlowableEventBuilder.createSignalEvent(FlowableEngineEventType.ACTIVITY_SIGNALED, execution.getCurrentActivityId(), null,
                            null, execution.getId(), execution.getProcessInstanceId(), execution.getProcessDefinitionId()));

            CommandContextUtil.getAgenda(commandContext).planTriggerExecutionOperation(execution);
            
        } else {
            CommandContextUtil.getAgenda(commandContext).planAsyncTriggerExecutionOperation(execution);
        }

        return null;
    }

    @Override
    protected String getSuspendedExceptionMessage() {
        return "Cannot trigger an execution that is suspended";
    }

}
