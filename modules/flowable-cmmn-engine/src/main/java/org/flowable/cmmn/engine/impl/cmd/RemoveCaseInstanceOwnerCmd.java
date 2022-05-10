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
 * Command to remove the current owner from a case instance.
 *
 * @author Micha Kiener
 */
public class RemoveCaseInstanceOwnerCmd extends AbstractCaseInstanceIdentityLinkCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected final String caseInstanceId;

    public RemoveCaseInstanceOwnerCmd(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        removeIdentityLinkType(caseInstanceId, IdentityLinkType.OWNER);
        return null;
    }
}
