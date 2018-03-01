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

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.variable.api.type.VariableScopeType;

/**
 * @author Tijs Rademakers
 */
public class GetIdentityLinksForCaseInstanceCmd implements Command<List<IdentityLink>>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String caseInstanceId;

    public GetIdentityLinksForCaseInstanceCmd(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public List<IdentityLink> execute(CommandContext commandContext) {
        CaseInstance caseInstance = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseInstanceId);

        if (caseInstance == null) {
            throw new FlowableObjectNotFoundException("Cannot find case instance with id " + caseInstanceId, CaseInstanceEntity.class);
        }

        return (List) CommandContextUtil.getIdentityLinkService(commandContext).findIdentityLinksByScopeIdAndType(
                        caseInstanceId, VariableScopeType.CMMN);
    }

}
