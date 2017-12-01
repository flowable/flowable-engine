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
package org.flowable.app.service.runtime;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.flowable.app.model.common.ResultListDataRepresentation;
import org.flowable.app.model.runtime.CaseDefinitionRepresentation;
import org.flowable.app.service.exception.BadRequestException;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
@Service
@Transactional
public class FlowableCaseDefinitionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableCaseDefinitionService.class);

    @Autowired
    protected CmmnRepositoryService cmmnRepositoryService;

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected ObjectMapper objectMapper;

    public ResultListDataRepresentation getCaseDefinitions(Boolean latest, String deploymentKey) {

        CaseDefinitionQuery definitionQuery = cmmnRepositoryService.createCaseDefinitionQuery();

        if (deploymentKey != null) {
            CmmnDeployment deployment = cmmnRepositoryService.createDeploymentQuery().deploymentKey(deploymentKey).latest().singleResult();

            if (deployment != null) {
                definitionQuery.deploymentId(deployment.getId());
            } else {
                return new ResultListDataRepresentation(new ArrayList<CaseDefinitionRepresentation>());
            }

        } else {

            if (latest != null && latest) {
                definitionQuery.latestVersion();
            }
        }

        List<CaseDefinition> definitions = definitionQuery.list();

        ResultListDataRepresentation result = new ResultListDataRepresentation(convertDefinitionList(definitions));
        return result;
    }

    protected CaseDefinition getCaseDefinitionFromRequest(String[] requestInfoArray, boolean isTableRequest) {
        int paramPosition = requestInfoArray.length - 3;
        if (isTableRequest) {
            paramPosition--;
        }
        String caseDefinitionId = getCaseDefinitionId(requestInfoArray, paramPosition);

        CaseDefinition caseDefinition = cmmnRepositoryService.getCaseDefinition(caseDefinitionId);

        return caseDefinition;
    }

    protected List<CaseDefinitionRepresentation> convertDefinitionList(List<CaseDefinition> definitions) {
        List<CaseDefinitionRepresentation> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(definitions)) {
            for (CaseDefinition caseDefinition : definitions) {
                CaseDefinitionRepresentation rep = new CaseDefinitionRepresentation(caseDefinition);
                result.add(rep);
            }
        }
        return result;
    }

    protected String[] parseRequest(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String[] requestInfoArray = requestURI.split("/");
        if (requestInfoArray.length < 2) {
            throw new BadRequestException("Start form request is not valid " + requestURI);
        }
        return requestInfoArray;
    }

    protected String getCaseDefinitionId(String[] requestInfoArray, int position) {
        String caseDefinitionVariable = requestInfoArray[position];
        String caseDefinitionId = null;
        try {
            caseDefinitionId = URLDecoder.decode(caseDefinitionVariable, "UTF-8");
        } catch (Exception e) {
            LOGGER.error("Error decoding case definition {}", caseDefinitionVariable, e);
            throw new InternalServerErrorException("Error decoding case definition " + caseDefinitionVariable);
        }
        return caseDefinitionId;
    }
}
