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
package org.flowable.ui.task.service.runtime;

import java.util.ArrayList;
import java.util.List;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.idm.api.User;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.flowable.ui.task.model.runtime.CaseInstanceRepresentation;
import org.flowable.ui.task.service.api.UserCache;
import org.flowable.ui.task.service.api.UserCache.CachedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
@Service
@Transactional
public class FlowableCaseInstanceQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableCaseInstanceQueryService.class);

    private static final int DEFAULT_PAGE_SIZE = 25;

    @Autowired
    protected CmmnRepositoryService cmmnRepositoryService;
    
    @Autowired
    protected AppRepositoryService appRepositoryService;

    @Autowired
    protected CmmnHistoryService cmmnHistoryService;

    @Autowired
    protected UserCache userCache;

    public ResultListDataRepresentation getCaseInstances(ObjectNode requestNode) {

        HistoricCaseInstanceQuery instanceQuery = cmmnHistoryService.createHistoricCaseInstanceQuery();

        // Case definition
        JsonNode caseDefinitionIdNode = requestNode.get("caseDefinitionId");
        if (caseDefinitionIdNode != null && !caseDefinitionIdNode.isNull()) {
            instanceQuery.caseDefinitionId(caseDefinitionIdNode.asText());
        }

        JsonNode appDefinitionKeyNode = requestNode.get("appDefinitionKey");
        if (appDefinitionKeyNode != null && !appDefinitionKeyNode.isNull()) {
            // Results need to be filtered in an app-context. We need to fetch the deployment id for this app and use that in the query
            List<AppDefinition> appDefinitions = appRepositoryService.createAppDefinitionQuery().appDefinitionKey(appDefinitionKeyNode.asText()).list();
            List<String> parentDeploymentIds = new ArrayList<>();
            for (AppDefinition appDefinition : appDefinitions) {
                parentDeploymentIds.add(appDefinition.getDeploymentId());
            }
            
            List<CmmnDeployment> deployments = cmmnRepositoryService.createDeploymentQuery().parentDeploymentIds(parentDeploymentIds).list();

            List<String> deploymentIds = new ArrayList<>();
            for (CmmnDeployment deployment : deployments) {
                deploymentIds.add(deployment.getId());
            }

            if (deploymentIds.size() > 0) {
                instanceQuery.deploymentIds(deploymentIds);
            } else {
                return new ResultListDataRepresentation(new ArrayList<CaseInstanceRepresentation>());
            }
        }

        // State filtering
        JsonNode stateNode = requestNode.get("state");
        if (stateNode != null && !stateNode.isNull()) {
            String state = stateNode.asText();
            if ("running".equals(state)) {
                instanceQuery.unfinished();
            } else if ("completed".equals(state)) {
                instanceQuery.finished();
            } else if (!"all".equals(state)) {
                throw new BadRequestException("Illegal state filter value passed, only 'running', 'completed' or 'all' are supported");
            }
        } else {
            // Default filtering, only running
            instanceQuery.unfinished();
        }

        // Sort and ordering
        JsonNode sortNode = requestNode.get("sort");
        if (sortNode != null && !sortNode.isNull()) {

            if ("created-desc".equals(sortNode.asText())) {
                instanceQuery.orderByStartTime().desc();
            } else if ("created-asc".equals(sortNode.asText())) {
                instanceQuery.orderByStartTime().asc();
            } else if ("ended-desc".equals(sortNode.asText())) {
                instanceQuery.orderByEndTime().desc();
            } else if ("ended-asc".equals(sortNode.asText())) {
                instanceQuery.orderByEndTime().asc();
            }

        } else {
            // Revert to default
            instanceQuery.orderByStartTime().desc();
        }

        int page = 0;
        JsonNode pageNode = requestNode.get("page");
        if (pageNode != null && !pageNode.isNull()) {
            page = pageNode.asInt(0);
        }

        int size = DEFAULT_PAGE_SIZE;
        JsonNode sizeNode = requestNode.get("size");
        if (sizeNode != null && !sizeNode.isNull()) {
            size = sizeNode.asInt(DEFAULT_PAGE_SIZE);
        }

        List<HistoricCaseInstance> instances = instanceQuery.listPage(page * size, size);
        ResultListDataRepresentation result = new ResultListDataRepresentation(convertInstanceList(instances));

        // In case we're not on the first page and the size exceeds the page size, we need to do an additional count for the total
        if (page != 0 || instances.size() == size) {
            Long totalCount = instanceQuery.count();
            result.setTotal(Long.valueOf(totalCount.intValue()));
            result.setStart(page * size);
        }
        return result;
    }

    protected List<CaseInstanceRepresentation> convertInstanceList(List<HistoricCaseInstance> instances) {
        List<CaseInstanceRepresentation> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(instances)) {

            for (HistoricCaseInstance caseInstance : instances) {
                User userRep = null;
                if (caseInstance.getStartUserId() != null) {
                    CachedUser user = userCache.getUser(caseInstance.getStartUserId());
                    if (user != null && user.getUser() != null) {
                        userRep = user.getUser();
                    }
                }

                CaseDefinition caseDefinition = cmmnRepositoryService.getCaseDefinition(caseInstance.getCaseDefinitionId());
                CaseInstanceRepresentation instanceRepresentation = new CaseInstanceRepresentation(caseInstance, caseDefinition, 
                                caseDefinition.hasGraphicalNotation(), userRep);
                result.add(instanceRepresentation);
            }

        }
        return result;
    }
}
