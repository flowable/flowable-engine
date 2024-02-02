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

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.repository.CmmnDeploymentQuery;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CmmnDeploymentDataManager;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.repository.CmmnDeploymentQueryImpl;
import org.flowable.cmmn.engine.impl.util.CmmnCorrelationUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;

/**
 * @author Joram Barrez
 */
public class CmmnDeploymentEntityManagerImpl
    extends AbstractEngineEntityManager<CmmnEngineConfiguration, CmmnDeploymentEntity, CmmnDeploymentDataManager>
    implements CmmnDeploymentEntityManager {

    public CmmnDeploymentEntityManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration, CmmnDeploymentDataManager deploymentDataManager) {
        super(cmmnEngineConfiguration, deploymentDataManager);
    }

    @Override
    public void insert(CmmnDeploymentEntity deployment) {
        super.insert(deployment, true);

        for (EngineResource resource : deployment.getResources().values()) {
            resource.setDeploymentId(deployment.getId());
            getCmmnResourceEntityManager().insert((CmmnResourceEntity) resource);
        }
    }

    @Override
    public void deleteDeploymentAndRelatedData(String deploymentId, boolean cascade) {
        CaseDefinitionEntityManager caseDefinitionEntityManager = getCaseDefinitionEntityManager();
        List<CaseDefinition> caseDefinitions = caseDefinitionEntityManager.createCaseDefinitionQuery().deploymentId(deploymentId).list();
        for (CaseDefinition caseDefinition : caseDefinitions) {
            engineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                .deleteIdentityLinksByScopeDefinitionIdAndType(caseDefinition.getId(), ScopeTypes.CMMN);
            engineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService()
                .deleteEventSubscriptionsForScopeDefinitionIdAndType(caseDefinition.getId(), ScopeTypes.CMMN);
            
            if (cascade) {
                caseDefinitionEntityManager.deleteCaseDefinitionAndRelatedData(caseDefinition.getId(), true);
            } else {
                caseDefinitionEntityManager.delete(caseDefinition.getId());
            }

            // If previous case definition version has an event registry start event, it must be added
            // Only if the currently deleted case definition is the latest version,
            // we fall back to the previous event registry start event
            restorePreviousStartEventsIfNeeded(caseDefinition);
        }
        getCmmnResourceEntityManager().deleteResourcesByDeploymentId(deploymentId);
        delete(findById(deploymentId));
    }

    protected void restorePreviousStartEventsIfNeeded(CaseDefinition caseDefinition) {
        CaseDefinitionEntity latestCaseDefinition = findLatestCaseDefinition(caseDefinition);
        if (latestCaseDefinition != null && caseDefinition.getId().equals(latestCaseDefinition.getId())) {

            // Try to find a previous version (it could be some versions are missing due to deletions)
            CaseDefinition previousCaseDefinition = findNewLatestCaseDefinitionAfterRemovalOf(caseDefinition);
            if (previousCaseDefinition != null) {
                CmmnModel cmmnModel = CaseDefinitionUtil.getCmmnModel(caseDefinition.getId());
                Case caseModel = cmmnModel.getPrimaryCase();
                String startEventType = caseModel.getStartEventType();
                if (startEventType != null) {
                    restoreEventRegistryStartEvent(previousCaseDefinition, caseModel, startEventType);
                }
            }
        }
    }

    protected void restoreEventRegistryStartEvent(CaseDefinition previousCaseDefinition, Case caseModel, String startEventType) {
        engineConfiguration.getEventSubscriptionServiceConfiguration()
                .getEventSubscriptionService()
                .createEventSubscriptionBuilder()
                .eventType(startEventType)
                .configuration(CmmnCorrelationUtil.getCorrelationKey(CmmnXmlConstants.ELEMENT_EVENT_CORRELATION_PARAMETER, CommandContextUtil.getCommandContext(), caseModel))
                .scopeDefinitionId(previousCaseDefinition.getId())
                .scopeType(ScopeTypes.CMMN)
                .tenantId(previousCaseDefinition.getTenantId())
                .create();
    }

    protected CaseDefinitionEntity findLatestCaseDefinition(CaseDefinition caseDefinition) {
        CaseDefinitionEntity latestCaseDefinition = null;
        if (caseDefinition.getTenantId() != null && !CmmnEngineConfiguration.NO_TENANT_ID.equals(caseDefinition.getTenantId())) {
            latestCaseDefinition = getCaseDefinitionEntityManager()
                    .findLatestCaseDefinitionByKeyAndTenantId(caseDefinition.getKey(), caseDefinition.getTenantId());
        } else {
            latestCaseDefinition = getCaseDefinitionEntityManager()
                    .findLatestCaseDefinitionByKey(caseDefinition.getKey());
        }
        return latestCaseDefinition;
    }

    protected CaseDefinition findNewLatestCaseDefinitionAfterRemovalOf(CaseDefinition caseDefinitionToBeRemoved) {

        // The case definition is not necessarily the one with 'version -1' (some versions could have been deleted)
        // Hence, the following logic

        CaseDefinitionQuery query = getCaseDefinitionEntityManager().createCaseDefinitionQuery();
        query.caseDefinitionKey(caseDefinitionToBeRemoved.getKey());

        if (caseDefinitionToBeRemoved.getTenantId() != null
                && !CmmnEngineConfiguration.NO_TENANT_ID.equals(caseDefinitionToBeRemoved.getTenantId())) {
            query.caseDefinitionTenantId(caseDefinitionToBeRemoved.getTenantId());
        } else {
            query.caseDefinitionWithoutTenantId();
        }

        if (caseDefinitionToBeRemoved.getVersion() > 0) {
            query.caseDefinitionVersionLowerThan(caseDefinitionToBeRemoved.getVersion());
        }
        query.orderByCaseDefinitionVersion().desc();

        List<CaseDefinition> caseDefinitions = query.listPage(0, 1);
        if (caseDefinitions != null && caseDefinitions.size() > 0) {
            return caseDefinitions.get(0);
        }
        return null;
    }

    @Override
    public CmmnDeploymentEntity findLatestDeploymentByName(String deploymentName) {
        return dataManager.findLatestDeploymentByName(deploymentName);
    }
    
    @Override
    public List<String> getDeploymentResourceNames(String deploymentId) {
        return dataManager.getDeploymentResourceNames(deploymentId);
    }
    
    @Override
    public CmmnDeploymentQuery createDeploymentQuery() {
        return new CmmnDeploymentQueryImpl(engineConfiguration.getCommandExecutor());
    }
    
    @Override
    public List<CmmnDeployment> findDeploymentsByQueryCriteria(CmmnDeploymentQuery deploymentQuery) {
        return dataManager.findDeploymentsByQueryCriteria((CmmnDeploymentQueryImpl) deploymentQuery);
    }
    
    @Override
    public long findDeploymentCountByQueryCriteria(CmmnDeploymentQuery deploymentQuery) {
        return dataManager.findDeploymentCountByQueryCriteria((CmmnDeploymentQueryImpl) deploymentQuery);
    }

    protected CmmnResourceEntityManager getCmmnResourceEntityManager() {
        return engineConfiguration.getCmmnResourceEntityManager();
    }

    protected CaseDefinitionEntityManager getCaseDefinitionEntityManager() {
        return engineConfiguration.getCaseDefinitionEntityManager();
    }

}
