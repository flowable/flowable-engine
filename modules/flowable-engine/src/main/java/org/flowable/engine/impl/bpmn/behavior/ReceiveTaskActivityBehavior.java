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

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * A receive task is a wait state that waits to receive some message.
 * 
 * Currently, the only message that is supported is the external trigger, given by calling the {@link RuntimeService#trigger(String)} operation.
 * 
 * @author Joram Barrez
 */
public class ReceiveTaskActivityBehavior extends TaskActivityBehavior {

    private static final long serialVersionUID = 1L;
    
    protected String receiveTaskId;
    protected String skipExpression;
    
    public ReceiveTaskActivityBehavior(String receiveTaskId, String skipExpression) {
        this.receiveTaskId = receiveTaskId;
        this.skipExpression = skipExpression;
    }

    @Override
    public void execute(DelegateExecution execution) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        boolean isSkipExpressionEnabled = SkipExpressionUtil.isSkipExpressionEnabled(skipExpression, receiveTaskId, execution, commandContext);

        if (isSkipExpressionEnabled && SkipExpressionUtil.shouldSkipFlowElement(skipExpression, receiveTaskId, execution, commandContext)) {
            leave(execution);
            return;
        }
    }

    @Override
    public void trigger(DelegateExecution execution, String signalName, Object data) {
        leave(execution);
    }

}
