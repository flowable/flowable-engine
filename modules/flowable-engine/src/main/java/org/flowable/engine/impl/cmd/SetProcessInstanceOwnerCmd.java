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

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.identitylink.api.IdentityLinkType;


/**
 * Command to set a new owner on a process instance and return the previous one, if any.
 *
 * @author Micha Kiener
 */
public class SetProcessInstanceOwnerCmd extends AbstractProcessInstanceIdentityLinkCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected final String processInstanceId;
    protected final String ownerUserId;

    public SetProcessInstanceOwnerCmd(String processInstanceId, String ownerUserId) {
        this.processInstanceId = processInstanceId;
        this.ownerUserId = ownerUserId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        removeIdentityLinkType(processInstanceId, IdentityLinkType.OWNER);
        getRuntimeService().addUserIdentityLink(processInstanceId, ownerUserId, IdentityLinkType.OWNER);
        return null;
    }
}