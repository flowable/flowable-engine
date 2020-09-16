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

package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryHelper;
import org.flowable.cmmn.engine.impl.history.HistoricCaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.history.HistoricMilestoneInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CaseDefinitionDataManager;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionQueryImpl;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntityManager;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityManager;


/**
 * @author Joram Barrez
 */
public class CaseDefinitionEntityManagerImpl
    extends AbstractEngineEntityManager<CmmnEngineConfiguration, CaseDefinitionEntity, CaseDefinitionDataManager>
    implements CaseDefinitionEntityManager {

    public CaseDefinitionEntityManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration, CaseDefinitionDataManager caseDefinitionDataManager) {
        super(cmmnEngineConfiguration, caseDefinitionDataManager);
    }

    @Override
    public CaseDefinitionEntity findLatestCaseDefinitionByKey(String caseDefinitionKey) {
        return dataManager.findLatestCaseDefinitionByKey(caseDefinitionKey);
    }

    @Override
    public CaseDefinitionEntity findLatestCaseDefinitionByKeyAndTenantId(String caseDefinitionKey, String tenantId) {
        return dataManager.findLatestCaseDefinitionByKeyAndTenantId(caseDefinitionKey, tenantId);
    }

    @Override
    public CaseDefinitionEntity findCaseDefinitionByDeploymentAndKey(String deploymentId, String caseDefinitionKey) {
        return dataManager.findCaseDefinitionByDeploymentAndKey(deploymentId, caseDefinitionKey);
    }

    @Override
    public CaseDefinitionEntity findCaseDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String caseDefinitionKey, String tenantId) {
        return dataManager.findCaseDefinitionByDeploymentAndKeyAndTenantId(deploymentId, caseDefinitionKey, tenantId);
    }

    @Override
    public CaseDefinitionEntity findCaseDefinitionByParentDeploymentAndKey(String parentDeploymentId, String caseDefinitionKey) {
        return dataManager.findCaseDefinitionByParentDeploymentAndKey(parentDeploymentId, caseDefinitionKey);
    }

    @Override
    public CaseDefinitionEntity findCaseDefinitionByParentDeploymentAndKeyAndTenantId(String parentDeploymentId, String caseDefinitionKey, String tenantId) {
        return dataManager.findCaseDefinitionByParentDeploymentAndKeyAndTenantId(parentDeploymentId, caseDefinitionKey, tenantId);
    }

    @Override
    public CaseDefinition findCaseDefinitionByKeyAndVersionAndTenantId(String caseDefinitionKey, Integer caseDefinitionVersion, String tenantId) {
        if (tenantId == null || CmmnEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            return dataManager.findCaseDefinitionByKeyAndVersion(caseDefinitionKey, caseDefinitionVersion);
        } else {
            return dataManager.findCaseDefinitionByKeyAndVersionAndTenantId(caseDefinitionKey, caseDefinitionVersion, tenantId);
        }
    }
    
    @Override
    public void deleteCaseDefinitionAndRelatedData(String caseDefinitionId, boolean cascadeHistory) {
        
        // Case instances
        CaseInstanceEntityManager caseInstanceEntityManager = getCaseInstanceEntityManager();
        CommandContext commandContext = Context.getCommandContext();
        List<CaseInstance> caseInstances = caseInstanceEntityManager.findByCriteria(
                new CaseInstanceQueryImpl(commandContext, engineConfiguration).caseDefinitionId(caseDefinitionId));
        for (CaseInstance caseInstance : caseInstances) {
            caseInstanceEntityManager.delete(caseInstance.getId(), true, null);
        }
        
        if (cascadeHistory) {
            engineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForScopeDefinition(ScopeTypes.CMMN, caseDefinitionId);

            HistoricIdentityLinkEntityManager historicIdentityLinkEntityManager = getHistoricIdentityLinkEntityManager();
            historicIdentityLinkEntityManager.deleteHistoricIdentityLinksByScopeDefinitionIdAndScopeType(caseDefinitionId, ScopeTypes.CMMN);
            
            // Historic milestone
            HistoricMilestoneInstanceEntityManager historicMilestoneInstanceEntityManager = getHistoricMilestoneInstanceEntityManager();
            List<HistoricMilestoneInstance> historicMilestoneInstances = historicMilestoneInstanceEntityManager
                    .findHistoricMilestoneInstancesByQueryCriteria(new HistoricMilestoneInstanceQueryImpl().milestoneInstanceCaseDefinitionId(caseDefinitionId));
            for (HistoricMilestoneInstance historicMilestoneInstance : historicMilestoneInstances) {
                historicMilestoneInstanceEntityManager.delete(historicMilestoneInstance.getId());
            }

            // Historic tasks
            HistoricTaskInstanceEntityManager historicTaskInstanceEntityManager = getHistoricTaskInstanceEntityManager();
            List<HistoricTaskInstance> historicTaskInstances = historicTaskInstanceEntityManager
                    .findHistoricTaskInstancesByQueryCriteria(new HistoricTaskInstanceQueryImpl().scopeDefinitionId(caseDefinitionId).scopeType(ScopeTypes.CMMN)); 
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                TaskHelper.deleteHistoricTask(historicTaskInstance.getId(), engineConfiguration);
            }

            // Historic Plan Items
            HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = getHistoricPlanItemInstanceEntityManager();
            historicPlanItemInstanceEntityManager.findByCaseDefinitionId(caseDefinitionId)
                    .forEach(p -> historicPlanItemInstanceEntityManager.delete(p.getId()));

            HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = getHistoricCaseInstanceEntityManager();
            List<HistoricCaseInstance> historicCaseInstanceEntities = historicCaseInstanceEntityManager
                    .findByCriteria(new HistoricCaseInstanceQueryImpl().caseDefinitionId(caseDefinitionId));
            for (HistoricCaseInstance historicCaseInstanceEntity : historicCaseInstanceEntities) {
                CmmnHistoryHelper.deleteHistoricCaseInstance(engineConfiguration, historicCaseInstanceEntity.getId());
            }
        }
        
        CaseDefinitionEntity caseDefinitionEntity = findById(caseDefinitionId);
        delete(caseDefinitionEntity);
    }
    
    @Override
    public CaseDefinitionQuery createCaseDefinitionQuery() {
        return new CaseDefinitionQueryImpl(engineConfiguration.getCommandExecutor());
    }

    @Override
    public List<CaseDefinition> findCaseDefinitionsByQueryCriteria(CaseDefinitionQuery caseDefinitionQuery) {
        return dataManager.findCaseDefinitionsByQueryCriteria((CaseDefinitionQueryImpl) caseDefinitionQuery);
    }

    @Override
    public long findCaseDefinitionCountByQueryCriteria(CaseDefinitionQuery caseDefinitionQuery) {
        return dataManager.findCaseDefinitionCountByQueryCriteria((CaseDefinitionQueryImpl) caseDefinitionQuery);
    }

    protected CaseInstanceEntityManager getCaseInstanceEntityManager() {
        return engineConfiguration.getCaseInstanceEntityManager();
    }

    protected HistoricIdentityLinkEntityManager getHistoricIdentityLinkEntityManager() {
        return engineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkEntityManager();
    }

    protected HistoricMilestoneInstanceEntityManager getHistoricMilestoneInstanceEntityManager() {
        return engineConfiguration.getHistoricMilestoneInstanceEntityManager();
    }

    protected HistoricTaskInstanceEntityManager getHistoricTaskInstanceEntityManager() {
        return engineConfiguration.getTaskServiceConfiguration().getHistoricTaskInstanceEntityManager();
    }

    protected HistoricPlanItemInstanceEntityManager getHistoricPlanItemInstanceEntityManager() {
        return engineConfiguration.getHistoricPlanItemInstanceEntityManager();
    }

    protected HistoricCaseInstanceEntityManager getHistoricCaseInstanceEntityManager() {
        return engineConfiguration.getHistoricCaseInstanceEntityManager();
    }

}
