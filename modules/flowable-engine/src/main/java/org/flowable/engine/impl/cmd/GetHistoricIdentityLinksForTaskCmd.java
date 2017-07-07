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
import java.util.List;

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.history.HistoricIdentityLink;
import org.flowable.engine.history.HistoricTaskInstance;
import org.flowable.engine.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.task.IdentityLinkType;

/**
 * @author Frederik Heremans
 */
public class GetHistoricIdentityLinksForTaskCmd implements Command<List<HistoricIdentityLink>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String taskId;
    protected String processInstanceId;

    public GetHistoricIdentityLinksForTaskCmd(String taskId, String processInstanceId) {
        if (taskId == null && processInstanceId == null) {
            throw new FlowableIllegalArgumentException("taskId or processInstanceId is required");
        }
        this.taskId = taskId;
        this.processInstanceId = processInstanceId;
    }

    public List<HistoricIdentityLink> execute(CommandContext commandContext) {
        if (taskId != null) {
            return getLinksForTask(commandContext);
        } else {
            return getLinksForProcessInstance(commandContext);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<HistoricIdentityLink> getLinksForTask(CommandContext commandContext) {
        HistoricTaskInstanceEntity task = CommandContextUtil.getHistoricTaskInstanceEntityManager(commandContext).findById(taskId);

        if (task == null) {
            throw new FlowableObjectNotFoundException("No historic task exists with the given id: " + taskId, HistoricTaskInstance.class);
        }

        List<HistoricIdentityLink> identityLinks = (List) CommandContextUtil.getHistoricIdentityLinkEntityManager(commandContext).findHistoricIdentityLinksByTaskId(taskId);

        HistoricIdentityLink assigneeIdentityLink = null;
        HistoricIdentityLink ownerIdentityLink = null;
        for (HistoricIdentityLink historicIdentityLink : identityLinks) {
            if (IdentityLinkType.ASSIGNEE.equals(historicIdentityLink.getType())) {
                assigneeIdentityLink = historicIdentityLink;

            } else if (IdentityLinkType.OWNER.equals(historicIdentityLink.getType())) {
                ownerIdentityLink = historicIdentityLink;
            }
        }

        // Similar to GetIdentityLinksForTask, return assignee and owner as identity link
        if (task.getAssignee() != null && assigneeIdentityLink == null) {
            HistoricIdentityLinkEntity identityLink = CommandContextUtil.getHistoricIdentityLinkEntityManager(commandContext).create();
            identityLink.setUserId(task.getAssignee());
            identityLink.setTaskId(task.getId());
            identityLink.setType(IdentityLinkType.ASSIGNEE);
            identityLinks.add(identityLink);
        }

        if (task.getOwner() != null && ownerIdentityLink == null) {
            HistoricIdentityLinkEntity identityLink = CommandContextUtil.getHistoricIdentityLinkEntityManager(commandContext).create();
            identityLink.setTaskId(task.getId());
            identityLink.setUserId(task.getOwner());
            identityLink.setType(IdentityLinkType.OWNER);
            identityLinks.add(identityLink);
        }

        return identityLinks;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<HistoricIdentityLink> getLinksForProcessInstance(CommandContext commandContext) {
        return (List) CommandContextUtil.getHistoricIdentityLinkEntityManager(commandContext).findHistoricIdentityLinksByProcessInstanceId(processInstanceId);
    }

}
