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

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.entitylink.api.history.HistoricEntityLinkService;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;

/**
 * @author Tijs Rademakers
 */
public class DeleteRelatedDataOfRemovedHistoricCaseInstancesCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;
    
    @Override
    public Object execute(CommandContext commandContext) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        cmmnEngineConfiguration.getHistoricMilestoneInstanceEntityManager().deleteHistoricMilestoneInstancesForNonExistingCaseInstances();
        IdentityLinkServiceConfiguration identityLinkServiceConfiguration = cmmnEngineConfiguration.getIdentityLinkServiceConfiguration();
        identityLinkServiceConfiguration.getHistoricIdentityLinkService().deleteHistoricCaseIdentityLinksForNonExistingInstances();
        identityLinkServiceConfiguration.getHistoricIdentityLinkService().deleteHistoricTaskIdentityLinksForNonExistingInstances();
        HistoricEntityLinkService historicEntityLinkService = cmmnEngineConfiguration.getEntityLinkServiceConfiguration().getHistoricEntityLinkService();
        if (historicEntityLinkService != null) {
            historicEntityLinkService.deleteHistoricEntityLinksForNonExistingCaseInstances();
        }
        cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForNonExistingCaseInstances();
        cmmnEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableService().deleteHistoricVariableInstancesForNonExistingCaseInstances();

        return null;
    }

}
