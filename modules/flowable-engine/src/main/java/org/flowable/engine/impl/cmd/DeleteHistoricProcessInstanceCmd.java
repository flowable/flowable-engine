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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;

/**
 * @author Frederik Heremans
 */
public class DeleteHistoricProcessInstanceCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String processInstanceId;

    public DeleteHistoricProcessInstanceCmd(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public Object execute(CommandContext commandContext) {
        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("processInstanceId is null");
        }
        // Check if process instance is still running
        HistoricProcessInstance instance = CommandContextUtil.getHistoricProcessInstanceEntityManager(commandContext).findById(processInstanceId);

        if (instance == null) {
            throw new FlowableObjectNotFoundException("No historic process instance found with id: " + processInstanceId, HistoricProcessInstance.class);
        }
        if (instance.getEndTime() == null) {
            throw new FlowableException("Process instance is still running, cannot delete historic process instance: " + processInstanceId);
        }

        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, instance.getProcessDefinitionId())) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            compatibilityHandler.deleteHistoricProcessInstance(processInstanceId);
            return null;
        }

        CommandContextUtil.getHistoryManager(commandContext).recordProcessInstanceDeleted(processInstanceId, instance.getProcessDefinitionId());

        return null;
    }

}
