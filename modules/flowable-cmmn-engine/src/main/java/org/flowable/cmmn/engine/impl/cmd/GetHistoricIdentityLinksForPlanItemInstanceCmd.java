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

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.identitylink.api.history.HistoricIdentityLink;

/**
 * @author Tijs Rademakers
 */
public class GetHistoricIdentityLinksForPlanItemInstanceCmd implements Command<List<HistoricIdentityLink>>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected String planItemInstanceId;

    public GetHistoricIdentityLinksForPlanItemInstanceCmd(String planItemInstanceId) {
        if (planItemInstanceId == null) {
            throw new FlowableIllegalArgumentException("planItemInstanceId is required");
        }
        this.planItemInstanceId = planItemInstanceId;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public List<HistoricIdentityLink> execute(CommandContext commandContext) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        return (List) cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService()
                .findHistoricIdentityLinksBySubScopeIdAndScopeType(planItemInstanceId, ScopeTypes.PLAN_ITEM);
    }

}
