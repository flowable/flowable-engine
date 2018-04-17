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
import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.IdentityService;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.idm.api.User;

/**
 * @author Tijs Rademakers
 */
public class GetPotentialStarterUsersCmd implements Command<List<User>>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String processDefinitionId;

    public GetPotentialStarterUsersCmd(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public List<User> execute(CommandContext commandContext) {
        ProcessDefinitionEntity processDefinition = CommandContextUtil.getProcessDefinitionEntityManager(commandContext).findById(processDefinitionId);

        if (processDefinition == null) {
            throw new FlowableObjectNotFoundException("Cannot find process definition with id " + processDefinitionId, ProcessDefinition.class);
        }

        IdentityService identityService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getIdentityService();

        List<String> userIds = new ArrayList<>();
        List<IdentityLink> identityLinks = (List) processDefinition.getIdentityLinks();
        for (IdentityLink identityLink : identityLinks) {
            if (identityLink.getUserId() != null && identityLink.getUserId().length() > 0) {

                if (!userIds.contains(identityLink.getUserId())) {
                    userIds.add(identityLink.getUserId());
                }
            }
        }

        if (userIds.size() > 0) {
            return identityService.createUserQuery().userIds(userIds).list();

        } else {
            return new ArrayList<>();
        }
    }

}
