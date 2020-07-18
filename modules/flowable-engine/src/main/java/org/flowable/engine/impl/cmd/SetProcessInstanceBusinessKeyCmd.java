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


import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * {@link Command} that changes the business key of an existing process instance.
 * 
 * @author Tijs Rademakers
 */
public class SetProcessInstanceBusinessKeyCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    private final String processInstanceId;
    private final String businessKey;

    public SetProcessInstanceBusinessKeyCmd(String processInstanceId, String businessKey) {
        if (processInstanceId == null || processInstanceId.length() < 1) {
            throw new FlowableIllegalArgumentException("The process instance id is mandatory, but '" + processInstanceId + "' has been provided.");
        }
        if (businessKey == null) {
            throw new FlowableIllegalArgumentException("The business key is mandatory, but 'null' has been provided.");
        }

        this.processInstanceId = processInstanceId;
        this.businessKey = businessKey;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        ExecutionEntityManager executionManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity processInstance = executionManager.findById(processInstanceId);
        if (processInstance == null) {
            if (CommandContextUtil.getProcessEngineConfiguration(commandContext).isFlowable5CompatibilityEnabled()) {
                CommandContextUtil.getProcessEngineConfiguration(commandContext).getFlowable5CompatibilityHandler().updateBusinessKey(processInstanceId,
                        businessKey);
                return null;
            }
            
            throw new FlowableObjectNotFoundException("No process instance found for id = '" + processInstanceId + "'.", ProcessInstance.class);
        } else if (!processInstance.isProcessInstanceType()) {
            throw new FlowableIllegalArgumentException("A process instance id is required, but the provided id " + "'" + processInstanceId + "' " + "points to a child execution of process instance " + "'"
                    + processInstance.getProcessInstanceId() + "'. " + "Please invoke the " + getClass().getSimpleName() + " with a root execution id.");
        }

        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, processInstance.getProcessDefinitionId())) {
            CommandContextUtil.getProcessEngineConfiguration(commandContext).getFlowable5CompatibilityHandler().updateBusinessKey(processInstanceId, businessKey);
            return null;
        }

        executionManager.updateProcessInstanceBusinessKey(processInstance, businessKey);

        return null;
    }
}
