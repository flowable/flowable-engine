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

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkType;

/**
 * @author Filip Hrisafov
 */
public class GetEntityLinkChildrenWithSameRootAsCaseInstanceCmd implements Command<List<EntityLink>> {

    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    protected String caseInstanceId;

    public GetEntityLinkChildrenWithSameRootAsCaseInstanceCmd(String caseInstanceId, CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.caseInstanceId = caseInstanceId;
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public List<EntityLink> execute(CommandContext commandContext) {
        CaseInstance caseInstance = cmmnEngineConfiguration.getCaseInstanceEntityManager().findById(caseInstanceId);

        if (caseInstance == null) {
            throw new FlowableObjectNotFoundException("Cannot find case instance with id " + caseInstanceId, CaseInstanceEntity.class);
        }

        return cmmnEngineConfiguration.getEntityLinkServiceConfiguration().getEntityLinkService()
                .findEntityLinksWithSameRootScopeForScopeIdAndScopeType(caseInstanceId, ScopeTypes.CMMN, EntityLinkType.CHILD);
    }

}
