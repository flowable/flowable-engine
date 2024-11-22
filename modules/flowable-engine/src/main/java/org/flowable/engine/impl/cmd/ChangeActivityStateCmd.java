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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.dynamic.DynamicStateManager;
import org.flowable.engine.impl.runtime.ChangeActivityStateBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class ChangeActivityStateCmd implements Command<Void> {

    protected ChangeActivityStateBuilderImpl changeActivityStateBuilder;

    public ChangeActivityStateCmd(ChangeActivityStateBuilderImpl changeActivityStateBuilder) {
        this.changeActivityStateBuilder = changeActivityStateBuilder;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (changeActivityStateBuilder.getMoveExecutionIdList().isEmpty() && changeActivityStateBuilder.getMoveActivityIdList().isEmpty()
                && changeActivityStateBuilder.getEnableActivityIdList().isEmpty() && changeActivityStateBuilder.getTerminateActivityIdList().isEmpty()
                && changeActivityStateBuilder.getTerminateExecutionIdList().isEmpty()) {
            
            throw new FlowableIllegalArgumentException("No move, enable or terminate activity ids provided");

        } else if ((!changeActivityStateBuilder.getMoveActivityIdList().isEmpty() || !changeActivityStateBuilder.getEnableActivityIdList().isEmpty()) && changeActivityStateBuilder.getProcessInstanceId() == null) {
            throw new FlowableIllegalArgumentException("Process instance id is required");
        }

        DynamicStateManager dynamicStateManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDynamicStateManager();
        dynamicStateManager.moveExecutionState(changeActivityStateBuilder, commandContext);

        return null;
    }
}
