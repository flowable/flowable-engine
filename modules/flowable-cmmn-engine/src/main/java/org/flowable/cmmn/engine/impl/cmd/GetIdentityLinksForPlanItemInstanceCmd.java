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

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.identitylink.api.IdentityLink;

/**
 * @author Tijs Rademakers
 */
public class GetIdentityLinksForPlanItemInstanceCmd implements Command<List<IdentityLink>>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String planItemInstanceId;

    public GetIdentityLinksForPlanItemInstanceCmd(String planItemInstanceId) {
        this.planItemInstanceId = planItemInstanceId;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public List<IdentityLink> execute(CommandContext commandContext) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        PlanItemInstance planItemInstance = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(planItemInstanceId);

        if (planItemInstance == null) {
            throw new FlowableObjectNotFoundException("Cannot find plan item instance with id " + planItemInstanceId, PlanItemInstanceEntity.class);
        }

        return (List) cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                .findIdentityLinksBySubScopeIdAndType(planItemInstanceId, ScopeTypes.PLAN_ITEM);
    }

}
