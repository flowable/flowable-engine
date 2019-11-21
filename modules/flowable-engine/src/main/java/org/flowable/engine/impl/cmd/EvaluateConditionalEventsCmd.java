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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class EvaluateConditionalEventsCmd extends NeedsActiveExecutionCmd<Object> {

    private static final long serialVersionUID = 1L;

    protected Map<String, Object> processVariables;
    protected Map<String, Object> transientVariables;
    protected boolean async;

    public EvaluateConditionalEventsCmd(String processInstanceId, Map<String, Object> processVariables) {
        super(processInstanceId);
        this.processVariables = processVariables;
    }

    public EvaluateConditionalEventsCmd(String processInstanceId, Map<String, Object> processVariables, Map<String, Object> transientVariables) {
        this(processInstanceId, processVariables);
        this.transientVariables = transientVariables;
    }

    @Override
    protected Object execute(CommandContext commandContext, ExecutionEntity execution) {
        if (!execution.isProcessInstanceType()) {
            throw new FlowableException("Execution is not of type process instance");
        }
        
        if (processVariables != null) {
            execution.setVariables(processVariables);
        }

        if (transientVariables != null) {
            execution.setTransientVariables(transientVariables);
        }

        CommandContextUtil.getAgenda(commandContext).planEvaluateConditionalEventsOperation(execution);

        return null;
    }

    @Override
    protected String getSuspendedExceptionMessage() {
        return "Cannot evaluate conditions for an execution that is suspended";
    }

}
