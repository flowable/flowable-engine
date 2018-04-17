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
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionInfoEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionInfoEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class SaveProcessDefinitionInfoCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String processDefinitionId;
    protected ObjectNode infoNode;

    public SaveProcessDefinitionInfoCmd(String processDefinitionId, ObjectNode infoNode) {
        this.processDefinitionId = processDefinitionId;
        this.infoNode = infoNode;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (processDefinitionId == null) {
            throw new FlowableIllegalArgumentException("process definition id is null");
        }

        if (infoNode == null) {
            throw new FlowableIllegalArgumentException("process definition info node is null");
        }

        ProcessDefinitionInfoEntityManager definitionInfoEntityManager = CommandContextUtil.getProcessDefinitionInfoEntityManager(commandContext);
        ProcessDefinitionInfoEntity definitionInfoEntity = definitionInfoEntityManager.findProcessDefinitionInfoByProcessDefinitionId(processDefinitionId);
        if (definitionInfoEntity == null) {
            definitionInfoEntity = definitionInfoEntityManager.create();
            definitionInfoEntity.setProcessDefinitionId(processDefinitionId);
            CommandContextUtil.getProcessDefinitionInfoEntityManager().insertProcessDefinitionInfo(definitionInfoEntity);
        }

        try {
            ObjectWriter writer = CommandContextUtil.getProcessEngineConfiguration(commandContext).getObjectMapper().writer();
            CommandContextUtil.getProcessDefinitionInfoEntityManager().updateInfoJson(definitionInfoEntity.getId(), writer.writeValueAsBytes(infoNode));
        } catch (Exception e) {
            throw new FlowableException("Unable to serialize info node " + infoNode);
        }

        return null;
    }

}
