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

package org.flowable.cmmn.rest.service.api.repository;

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionQueryProperty;
import org.flowable.cmmn.rest.service.api.CmmnRestApiInterceptor;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Case Definitions" }, authorizations = { @Authorization(value = "basicAuth") })
public class CaseDefinitionCollectionResource {

    private static final Map<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("id", CaseDefinitionQueryProperty.CASE_DEFINITION_ID);
        properties.put("key", CaseDefinitionQueryProperty.CASE_DEFINITION_KEY);
        properties.put("category", CaseDefinitionQueryProperty.CASE_DEFINITION_CATEGORY);
        properties.put("name", CaseDefinitionQueryProperty.CASE_DEFINITION_NAME);
        properties.put("version", CaseDefinitionQueryProperty.CASE_DEFINITION_VERSION);
        properties.put("deploymentId", CaseDefinitionQueryProperty.CASE_DEFINITION_DEPLOYMENT_ID);
        properties.put("tenantId", CaseDefinitionQueryProperty.CASE_DEFINITION_TENANT_ID);
    }

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnRepositoryService repositoryService;
    
    @Autowired(required=false)
    protected CmmnRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "List of case definitions", tags = { "Case Definitions" }, nickname = "listCaseDefinitions")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "version", dataType = "integer", value = "Only return case definitions with the given version.", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return case definitions with the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return case definitions with a name like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLikeIgnoreCase", dataType = "string", value = "Only return case definitions with a name like the given name ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "key", dataType = "string", value = "Only return case definitions with the given key.", paramType = "query"),
            @ApiImplicitParam(name = "keyLike", dataType = "string", value = "Only return case definitions with a name like the given key.", paramType = "query"),
            @ApiImplicitParam(name = "resourceName", dataType = "string", value = "Only return case definitions with the given resource name.", paramType = "query"),
            @ApiImplicitParam(name = "resourceNameLike", dataType = "string", value = "Only return case definitions with a name like the given resource name.", paramType = "query"),
            @ApiImplicitParam(name = "category", dataType = "string", value = "Only return case definitions with the given category.", paramType = "query"),
            @ApiImplicitParam(name = "categoryLike", dataType = "string", value = "Only return case definitions with a category like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "categoryNotEquals", dataType = "string", value = "Only return case definitions which do not have the given category.", paramType = "query"),
            @ApiImplicitParam(name = "deploymentId", dataType = "string", value = "Only return case definitions which are part of a deployment with the given deployment id.", paramType = "query"),
            @ApiImplicitParam(name = "parentDeploymentId", dataType = "string", value = "Only return case definitions which are part of a deployment with the given parent deployment id.", paramType = "query"),
            @ApiImplicitParam(name = "startableByUser", dataType = "string", value = "Only return case definitions which are part of a deployment with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "latest", dataType = "boolean", value = "Only return the latest case definition versions. Can only be used together with key and keyLike parameters, using any other parameter will result in a 400-response.", paramType = "query"),
            @ApiImplicitParam(name = "suspended", dataType = "boolean", value = "If true, only returns case definitions which are suspended. If false, only active process definitions (which are not suspended) are returned.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "name,id,key,category,deploymentId,version", paramType = "query"),
            @ApiImplicitParam(name = "order", dataType = "string", value = "The sort order, either 'asc' or 'desc'. Defaults to 'asc'.", paramType = "query"),
            @ApiImplicitParam(name = "start", dataType = "integer", value = "Index of the first row to fetch. Defaults to 0.", paramType = "query"),
            @ApiImplicitParam(name = "size", dataType = "integer", value = "Number of rows to fetch, starting from start. Defaults to 10.", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the case definitions are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format or that latest is used with other parameters other than key and keyLike. The status-message contains additional information.")
    })
    @GetMapping(value = "/cmmn-repository/case-definitions", produces = "application/json")
    public DataResponse<CaseDefinitionResponse> getProcessDefinitions(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        CaseDefinitionQuery caseDefinitionQuery = repositoryService.createCaseDefinitionQuery();

        // Populate filter-parameters
        if (allRequestParams.containsKey("category")) {
            caseDefinitionQuery.caseDefinitionCategory(allRequestParams.get("category"));
        }
        if (allRequestParams.containsKey("categoryLike")) {
            caseDefinitionQuery.caseDefinitionCategoryLike(allRequestParams.get("categoryLike"));
        }
        if (allRequestParams.containsKey("categoryNotEquals")) {
            caseDefinitionQuery.caseDefinitionCategoryNotEquals(allRequestParams.get("categoryNotEquals"));
        }
        if (allRequestParams.containsKey("key")) {
            caseDefinitionQuery.caseDefinitionKey(allRequestParams.get("key"));
        }
        if (allRequestParams.containsKey("keyLike")) {
            caseDefinitionQuery.caseDefinitionKeyLike(allRequestParams.get("keyLike"));
        }
        if (allRequestParams.containsKey("name")) {
            caseDefinitionQuery.caseDefinitionName(allRequestParams.get("name"));
        }
        if (allRequestParams.containsKey("nameLike")) {
            caseDefinitionQuery.caseDefinitionNameLike(allRequestParams.get("nameLike"));
        }
        if (allRequestParams.containsKey("nameLikeIgnoreCase")) {
            caseDefinitionQuery.caseDefinitionNameLikeIgnoreCase(allRequestParams.get("nameLikeIgnoreCase"));
        }
        if (allRequestParams.containsKey("resourceName")) {
            caseDefinitionQuery.caseDefinitionResourceName(allRequestParams.get("resourceName"));
        }
        if (allRequestParams.containsKey("resourceNameLike")) {
            caseDefinitionQuery.caseDefinitionResourceNameLike(allRequestParams.get("resourceNameLike"));
        }
        if (allRequestParams.containsKey("version")) {
            caseDefinitionQuery.caseDefinitionVersion(Integer.valueOf(allRequestParams.get("version")));
        }
        if (allRequestParams.containsKey("latest")) {
            if (Boolean.parseBoolean(allRequestParams.get("latest"))) {
                caseDefinitionQuery.latestVersion();
            }
        }
        if (allRequestParams.containsKey("deploymentId")) {
            caseDefinitionQuery.deploymentId(allRequestParams.get("deploymentId"));
        }
        if (allRequestParams.containsKey("parentDeploymentId")) {
            caseDefinitionQuery.parentDeploymentId(allRequestParams.get("parentDeploymentId"));
        }
        if (allRequestParams.containsKey("startableByUser")) {
            caseDefinitionQuery.startableByUser(allRequestParams.get("startableByUser"));
        }
        if (allRequestParams.containsKey("tenantId")) {
            caseDefinitionQuery.caseDefinitionTenantId(allRequestParams.get("tenantId"));
        }
        if (allRequestParams.containsKey("tenantIdLike")) {
            caseDefinitionQuery.caseDefinitionTenantIdLike(allRequestParams.get("tenantIdLike"));
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessCaseDefinitionsWithQuery(caseDefinitionQuery);
        }

        return paginateList(allRequestParams, caseDefinitionQuery, "name", properties, restResponseFactory::createCaseDefinitionResponseList);
    }
}
