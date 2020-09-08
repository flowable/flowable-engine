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
package org.flowable.engine.impl.history.async.json.transformer;

import java.util.List;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityManager;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.entitylink.service.EntityLinkServiceConfiguration;

public abstract class AbstractProcessInstanceDeleteHistoryTransformer extends AbstractHistoryJsonTransformer {

    public AbstractProcessInstanceDeleteHistoryTransformer(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }
    
    protected void deleteProcessInstance(String processInstanceId, CommandContext commandContext) {
        HistoricProcessInstanceEntityManager historicProcessInstanceEntityManager = processEngineConfiguration.getHistoricProcessInstanceEntityManager();
        HistoricProcessInstanceEntity historicProcessInstance = historicProcessInstanceEntityManager.findById(processInstanceId);
        
        processEngineConfiguration.getHistoricDetailEntityManager().deleteHistoricDetailsByProcessInstanceId(processInstanceId);
        processEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableService().deleteHistoricVariableInstancesByProcessInstanceId(processInstanceId);
        processEngineConfiguration.getHistoricActivityInstanceEntityManager().deleteHistoricActivityInstancesByProcessInstanceId(processInstanceId);
        TaskHelper.deleteHistoricTaskInstancesByProcessInstanceId(processInstanceId);
        processEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService().deleteHistoricIdentityLinksByProcessInstanceId(processInstanceId);
        EntityLinkServiceConfiguration entityLinkServiceConfiguration = processEngineConfiguration.getEntityLinkServiceConfiguration();
        if (entityLinkServiceConfiguration != null) {
            entityLinkServiceConfiguration.getHistoricEntityLinkService().deleteHistoricEntityLinksByScopeIdAndScopeType(processInstanceId, ScopeTypes.BPMN);
        }
        processEngineConfiguration.getCommentEntityManager().deleteCommentsByProcessInstanceId(processInstanceId);

        historicProcessInstanceEntityManager.delete(historicProcessInstance, false);

        // Also delete any sub-processes that may be active (ACT-821)

        List<HistoricProcessInstance> selectList = historicProcessInstanceEntityManager.findHistoricProcessInstancesBySuperProcessInstanceId(processInstanceId);
        for (HistoricProcessInstance child : selectList) {
            deleteProcessInstance(child.getId(), commandContext);
        }
    }
}
