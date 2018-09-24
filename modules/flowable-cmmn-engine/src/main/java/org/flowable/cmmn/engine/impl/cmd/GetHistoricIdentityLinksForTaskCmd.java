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
package org.flowable.cmmn.engine.impl.cmd;

import java.io.Serializable;
import java.util.List;

import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.identitylink.service.HistoricIdentityLinkService;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;

/**
 * @author Tijs Rademakers
 */
public class GetHistoricIdentityLinksForTaskCmd implements Command<List<HistoricIdentityLink>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String taskId;

    public GetHistoricIdentityLinksForTaskCmd(String taskId) {
        if (taskId == null) {
            throw new FlowableIllegalArgumentException("taskId is required");
        }
        this.taskId = taskId;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<HistoricIdentityLink> execute(CommandContext commandContext) {
        HistoricTaskInstanceEntity task = CommandContextUtil.getHistoricTaskService().getHistoricTask(taskId);

        if (task == null) {
            throw new FlowableObjectNotFoundException("No historic task exists with the given id: " + taskId, HistoricTaskInstance.class);
        }

        HistoricIdentityLinkService historicIdentityLinkService = CommandContextUtil.getHistoricIdentityLinkService();
        List<HistoricIdentityLinkEntity> identityLinks = historicIdentityLinkService.findHistoricIdentityLinksByTaskId(taskId);

        HistoricIdentityLinkEntity assigneeIdentityLink = null;
        HistoricIdentityLinkEntity ownerIdentityLink = null;
        for (HistoricIdentityLinkEntity historicIdentityLink : identityLinks) {
            if (IdentityLinkType.ASSIGNEE.equals(historicIdentityLink.getType())) {
                assigneeIdentityLink = historicIdentityLink;

            } else if (IdentityLinkType.OWNER.equals(historicIdentityLink.getType())) {
                ownerIdentityLink = historicIdentityLink;
            }
        }

        // Similar to GetIdentityLinksForTask, return assignee and owner as identity link
        if (task.getAssignee() != null && assigneeIdentityLink == null) {
            HistoricIdentityLinkEntity identityLink = historicIdentityLinkService.createHistoricIdentityLink();
            identityLink.setUserId(task.getAssignee());
            identityLink.setTaskId(task.getId());
            identityLink.setType(IdentityLinkType.ASSIGNEE);
            identityLinks.add(identityLink);
        }

        if (task.getOwner() != null && ownerIdentityLink == null) {
            HistoricIdentityLinkEntity identityLink = historicIdentityLinkService.createHistoricIdentityLink();
            identityLink.setTaskId(task.getId());
            identityLink.setUserId(task.getOwner());
            identityLink.setType(IdentityLinkType.OWNER);
            identityLinks.add(identityLink);
        }

        return (List) identityLinks;
    }
}
