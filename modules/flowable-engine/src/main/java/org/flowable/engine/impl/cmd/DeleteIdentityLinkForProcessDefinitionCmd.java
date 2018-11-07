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
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * @author Tijs Rademakers
 */
public class DeleteIdentityLinkForProcessDefinitionCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String processDefinitionId;

    protected String userId;

    protected String groupId;

    public DeleteIdentityLinkForProcessDefinitionCmd(String processDefinitionId, String userId, String groupId) {
        validateParams(userId, groupId, processDefinitionId);
        this.processDefinitionId = processDefinitionId;
        this.userId = userId;
        this.groupId = groupId;
    }

    protected void validateParams(String userId, String groupId, String processDefinitionId) {
        if (processDefinitionId == null) {
            throw new FlowableIllegalArgumentException("processDefinitionId is null");
        }

        if (userId == null && groupId == null) {
            throw new FlowableIllegalArgumentException("userId and groupId cannot both be null");
        }
    }

    @Override
    public Void execute(CommandContext commandContext) {
        ProcessDefinitionEntity processDefinition = CommandContextUtil.getProcessDefinitionEntityManager(commandContext).findById(processDefinitionId);

        if (processDefinition == null) {
            throw new FlowableObjectNotFoundException("Cannot find process definition with id " + processDefinitionId, ProcessDefinition.class);
        }

        if (Flowable5Util.isFlowable5ProcessDefinition(processDefinition, commandContext)) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            compatibilityHandler.deleteCandidateStarter(processDefinitionId, userId, groupId);
            return null;
        }

        CommandContextUtil.getIdentityLinkService().deleteProcessDefinitionIdentityLink(processDefinition.getId(), userId, groupId);

        return null;
    }

}
