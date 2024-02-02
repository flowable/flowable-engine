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

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.identitylink.api.IdentityLinkType;

/**
 * Command to set a new assignee on a case instance.
 *
 * @author Micha Kiener
 */
public class SetCaseInstanceAssigneeCmd extends AbstractCaseInstanceIdentityLinkCmd implements Command<Void>, Serializable {
    private static final long serialVersionUID = 1L;

    protected final String caseInstanceId;
    protected final String assigneeUserId;

    public SetCaseInstanceAssigneeCmd(String caseInstanceId, String assigneeUserId) {
        this.caseInstanceId = caseInstanceId;
        this.assigneeUserId = assigneeUserId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        // remove ALL assignee identity links (there should only be one of course)
        removeIdentityLinkType(commandContext, caseInstanceId, IdentityLinkType.ASSIGNEE);

        // now add the new one, but only, if it is not null, which means using the setAssignee with a null value results in the same
        // as removeAssignee
        createIdentityLinkType(commandContext, caseInstanceId, assigneeUserId, null, IdentityLinkType.ASSIGNEE);
        return null;
    }
}
