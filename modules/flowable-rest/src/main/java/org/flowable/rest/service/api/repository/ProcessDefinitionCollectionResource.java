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

package org.flowable.rest.service.api.repository;

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.ProcessDefinitionQueryProperty;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
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
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Process Definitions" }, authorizations = { @Authorization(value = "basicAuth") })
public class ProcessDefinitionCollectionResource {

    private static final Map<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("id", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_ID);
        properties.put("key", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_KEY);
        properties.put("category", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_CATEGORY);
        properties.put("name", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_NAME);
        properties.put("version", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_VERSION);
        properties.put("deploymentId", ProcessDefinitionQueryProperty.DEPLOYMENT_ID);
        properties.put("tenantId", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_TENANT_ID);
    }

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected RepositoryService repositoryService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    @ApiOperation(value = "List of process definitions", tags = { "Process Definitions" }, nickname = "listProcessDefinitions")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "version", dataType = "integer", value = "Only return process definitions with the given version.", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return process definitions with the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return process definitions with a name like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLikeIgnoreCase", dataType = "string", value = "Only return process definitions with a name like the given name ignoring case.", paramType = "query"),
            @ApiImplicitParam(name = "key", dataType = "string", value = "Only return process definitions with the given key.", paramType = "query"),
            @ApiImplicitParam(name = "keyLike", dataType = "string", value = "Only return process definitions with a name like the given key.", paramType = "query"),
            @ApiImplicitParam(name = "resourceName", dataType = "string", value = "Only return process definitions with the given resource name.", paramType = "query"),
            @ApiImplicitParam(name = "resourceNameLike", dataType = "string", value = "Only return process definitions with a name like the given resource name.", paramType = "query"),
            @ApiImplicitParam(name = "category", dataType = "string", value = "Only return process definitions with the given category.", paramType = "query"),
            @ApiImplicitParam(name = "categoryLike", dataType = "string", value = "Only return process definitions with a category like the given name.", paramType = "query"),
            @ApiImplicitParam(name = "categoryNotEquals", dataType = "string", value = "Only return process definitions which do not have the given category.", paramType = "query"),
            @ApiImplicitParam(name = "deploymentId", dataType = "string", value = "Only return process definitions which are part of a deployment with the given deployment id.", paramType = "query"),
            @ApiImplicitParam(name = "parentDeploymentId", dataType = "string", value = "Only return process definitions which are part of a deployment with the given parent deployment id.", paramType = "query"),
            @ApiImplicitParam(name = "startableByUser", dataType = "string", value = "Only return process definitions which are part of a deployment with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "latest", dataType = "boolean", value = "Only return the latest process definition versions. Can only be used together with key and keyLike parameters, using any other parameter will result in a 400-response.", paramType = "query"),
            @ApiImplicitParam(name = "suspended", dataType = "boolean", value = "If true, only returns process definitions which are suspended. If false, only active process definitions (which are not suspended) are returned.", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", value = "Property to sort on, to be used together with the order.", allowableValues = "name,id,key,category,deploymentId,version", paramType = "query"),
            @ApiImplicitParam(name = "order", dataType = "string", value = "The sort order, either 'asc' or 'desc'. Defaults to 'asc'.", paramType = "query"),
            @ApiImplicitParam(name = "start", dataType = "integer", value = "Index of the first row to fetch. Defaults to 0.", paramType = "query"),
            @ApiImplicitParam(name = "size", dataType = "integer", value = "Number of rows to fetch, starting from start. Defaults to 10.", paramType = "query"),

    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates request was successful and the process-definitions are returned"),
            @ApiResponse(code = 400, message = "Indicates a parameter was passed in the wrong format or that latest is used with other parameters other than key and keyLike. The status-message contains additional information.")
    })
    @GetMapping(value = "/repository/process-definitions", produces = "application/json")
    public DataResponse<ProcessDefinitionResponse> getProcessDefinitions(@ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();

        // Populate filter-parameters
        if (allRequestParams.containsKey("category")) {
            processDefinitionQuery.processDefinitionCategory(allRequestParams.get("category"));
        }
        if (allRequestParams.containsKey("categoryLike")) {
            processDefinitionQuery.processDefinitionCategoryLike(allRequestParams.get("categoryLike"));
        }
        if (allRequestParams.containsKey("categoryNotEquals")) {
            processDefinitionQuery.processDefinitionCategoryNotEquals(allRequestParams.get("categoryNotEquals"));
        }
        if (allRequestParams.containsKey("key")) {
            processDefinitionQuery.processDefinitionKey(allRequestParams.get("key"));
        }
        if (allRequestParams.containsKey("keyLike")) {
            processDefinitionQuery.processDefinitionKeyLike(allRequestParams.get("keyLike"));
        }
        if (allRequestParams.containsKey("name")) {
            processDefinitionQuery.processDefinitionName(allRequestParams.get("name"));
        }
        if (allRequestParams.containsKey("nameLike")) {
            processDefinitionQuery.processDefinitionNameLike(allRequestParams.get("nameLike"));
        }
        if (allRequestParams.containsKey("nameLikeIgnoreCase")) {
            processDefinitionQuery.processDefinitionNameLikeIgnoreCase(allRequestParams.get("nameLikeIgnoreCase"));
        }
        if (allRequestParams.containsKey("resourceName")) {
            processDefinitionQuery.processDefinitionResourceName(allRequestParams.get("resourceName"));
        }
        if (allRequestParams.containsKey("resourceNameLike")) {
            processDefinitionQuery.processDefinitionResourceNameLike(allRequestParams.get("resourceNameLike"));
        }
        if (allRequestParams.containsKey("version")) {
            processDefinitionQuery.processDefinitionVersion(Integer.valueOf(allRequestParams.get("version")));
        }
        if (allRequestParams.containsKey("suspended")) {
            if (Boolean.parseBoolean(allRequestParams.get("suspended"))) {
                processDefinitionQuery.suspended();
            } else {
                processDefinitionQuery.active();
            }
        }
        if (allRequestParams.containsKey("latest")) {
            if (Boolean.parseBoolean(allRequestParams.get("latest"))) {
                processDefinitionQuery.latestVersion();
            }
        }
        if (allRequestParams.containsKey("deploymentId")) {
            processDefinitionQuery.deploymentId(allRequestParams.get("deploymentId"));
        }
        if (allRequestParams.containsKey("parentDeploymentId")) {
            processDefinitionQuery.parentDeploymentId(allRequestParams.get("parentDeploymentId"));
        }
        if (allRequestParams.containsKey("startableByUser")) {
            processDefinitionQuery.startableByUser(allRequestParams.get("startableByUser"));
        }
        if (allRequestParams.containsKey("tenantId")) {
            processDefinitionQuery.processDefinitionTenantId(allRequestParams.get("tenantId"));
        }
        if (allRequestParams.containsKey("tenantIdLike")) {
            processDefinitionQuery.processDefinitionTenantIdLike(allRequestParams.get("tenantIdLike"));
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessProcessDefinitionsWithQuery(processDefinitionQuery);
        }

        return paginateList(allRequestParams, processDefinitionQuery, "name", properties, restResponseFactory::createProcessDefinitionResponseList);
    }
}
