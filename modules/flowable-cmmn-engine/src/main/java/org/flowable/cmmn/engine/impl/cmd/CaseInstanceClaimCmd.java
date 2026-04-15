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
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.IdentityLinkUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;

public class CaseInstanceClaimCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    private final String caseInstanceId;
    private final String userId;

    public CaseInstanceClaimCmd(String caseInstanceId, String userId) {
        if (caseInstanceId == null || caseInstanceId.length() < 1) {
            throw new FlowableIllegalArgumentException("The case instance id is mandatory, but '" + caseInstanceId + "' has not been provided.");
        }

        this.caseInstanceId = caseInstanceId;
        this.userId = userId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        CaseInstanceEntity caseInstanceEntity = caseInstanceEntityManager.findById(caseInstanceId);
        if (caseInstanceEntity == null) {
            throw new FlowableObjectNotFoundException("No case instance found for id = '" + caseInstanceId + "'.", CaseInstance.class);
        }

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        if (userId != null) {
            List<IdentityLinkEntity> identityLinks = cmmnEngineConfiguration.getIdentityLinkServiceConfiguration()
                    .getIdentityLinkService().findIdentityLinksByScopeIdAndType(caseInstanceId, ScopeTypes.CMMN);
            for (IdentityLinkEntity identityLink : identityLinks) {
                if (IdentityLinkType.ASSIGNEE.equals(identityLink.getType())) {
                    throw new FlowableException("Case instance '" + caseInstanceId + "' is already claimed.");
                }
            }

            IdentityLinkUtil.createCaseInstanceIdentityLink(caseInstanceEntity, userId, null, IdentityLinkType.ASSIGNEE, cmmnEngineConfiguration);

            caseInstanceEntityManager.updateCaseInstanceClaimTime(caseInstanceEntity, cmmnEngineConfiguration.getClock().getCurrentTime(), userId);

            if (cmmnEngineConfiguration.getCaseInstanceStateInterceptor() != null) {
                cmmnEngineConfiguration.getCaseInstanceStateInterceptor().handleClaim(caseInstanceEntity, userId);
            }

        } else {
            IdentityLinkUtil.deleteCaseInstanceIdentityLinks(caseInstanceEntity, null, null, IdentityLinkType.ASSIGNEE, cmmnEngineConfiguration);

            caseInstanceEntityManager.updateCaseInstanceClaimTime(caseInstanceEntity, null, null);

            if (cmmnEngineConfiguration.getCaseInstanceStateInterceptor() != null) {
                cmmnEngineConfiguration.getCaseInstanceStateInterceptor().handleUnclaim(caseInstanceEntity, userId);
            }
        }

        return null;
    }
}
